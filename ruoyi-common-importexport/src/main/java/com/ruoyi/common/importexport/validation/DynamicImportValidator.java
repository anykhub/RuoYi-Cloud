package com.ruoyi.common.importexport.validation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.NullValueInNestedPathException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 动态导入数据校验引擎
 */
@Slf4j
@Component
public class DynamicImportValidator implements InitializingBean {

    private final Map<String, List<FieldRule>> classRulesMap = new HashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${importexport.rules.location:classpath*:import-rules.json}")
    private String rulesLocation;

    @Value("${importexport.rules.content:}")
    private String rulesContent;

    @Value("${importexport.rules.nacos-data-id:import-rules.json}")
    private String nacosDataId;

    @Autowired
    private Environment environment;

    @Override
    public void afterPropertiesSet() {
        loadRules();
    }

    /**
     * 加载动态校验规则 (优先从直接配置的内容读取，否则从 Nacos DataID 读取，最后从文件路径读取)
     */
    public void loadRules() {
        classRulesMap.clear();
        int totalRules = 0;

        try {
            // 1. 优先解析直接注入的 JSON 内容 (例如从 Nacos properties 中配置的 content)
            if (StringUtils.hasText(rulesContent)) {
                List<FieldRule> rules = objectMapper.readValue(rulesContent, new TypeReference<List<FieldRule>>() {});
                for (FieldRule rule : rules) {
                    classRulesMap.computeIfAbsent(rule.getClassName(), k -> new ArrayList<>()).add(rule);
                    totalRules++;
                }
                log.info("从直接配置的内容 (importexport.rules.content) 加载动态校验规则完成, 共 {} 条规则", totalRules);
                return;
            }

            // 2. 尝试解析从 Nacos 直接挂载为文件的独立配置 (通过 Environment 获取 Data ID 为键的值)
            if (StringUtils.hasText(nacosDataId)) {
                String nacosContent = environment.getProperty(nacosDataId);
                if (StringUtils.hasText(nacosContent)) {
                    List<FieldRule> rules = objectMapper.readValue(nacosContent, new TypeReference<List<FieldRule>>() {});
                    for (FieldRule rule : rules) {
                        classRulesMap.computeIfAbsent(rule.getClassName(), k -> new ArrayList<>()).add(rule);
                        totalRules++;
                    }
                    log.info("从 Nacos 独立文件配置 ({}) 加载动态校验规则完成, 共 {} 条规则", nacosDataId, totalRules);
                    return;
                }
            }

            // 3. 最后退化到通过路径解析 (文件路径扫描)
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(rulesLocation);

            if (resources.length == 0) {
                log.info("未配置直接规则内容，也未找到动态校验规则配置文件: {}", rulesLocation);
                return;
            }

            for (Resource resource : resources) {
                if (resource.exists()) {
                    try (InputStream is = resource.getInputStream()) {
                        List<FieldRule> rules = objectMapper.readValue(is, new TypeReference<List<FieldRule>>() {});
                        for (FieldRule rule : rules) {
                            classRulesMap.computeIfAbsent(rule.getClassName(), k -> new ArrayList<>()).add(rule);
                            totalRules++;
                        }
                    } catch (Exception ex) {
                        log.error("解析校验规则文件失败: " + resource.getFilename(), ex);
                    }
                }
            }
            log.info("从 {} 个文件加载动态校验规则完成, 共 {} 条规则", resources.length, totalRules);
        } catch (Exception e) {
            log.error("加载动态校验规则失败", e);
        }
    }

    /**
     * 校验目标对象
     *
     * @param target 目标对象
     * @return 错误信息列表
     */
    public List<String> validate(Object target) {
        List<String> errors = new ArrayList<>();
        if (target == null) {
            return errors;
        }

        String className = target.getClass().getName();
        List<FieldRule> rules = classRulesMap.get(className);

        if (rules == null || rules.isEmpty()) {
            return errors;
        }

        BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(target);

        for (FieldRule rule : rules) {
            try {
                Object value = beanWrapper.getPropertyValue(rule.getFieldName());

                // 校验必填
                if (Boolean.TRUE.equals(rule.getRequired())) {
                    if (value == null || (value instanceof String && !StringUtils.hasText((String) value))) {
                        errors.add(getMessage(rule, "不能为空"));
                        continue;
                    }
                }

                // 校验正则
                if (StringUtils.hasText(rule.getRegex()) && value != null) {
                    String strValue = String.valueOf(value);
                    if (!Pattern.matches(rule.getRegex(), strValue)) {
                        errors.add(getMessage(rule, "格式不正确"));
                    }
                }
            } catch (NullValueInNestedPathException e) {
                // 当中间路径对象为 null 时，说明最终属性也为空
                if (Boolean.TRUE.equals(rule.getRequired())) {
                    errors.add(getMessage(rule, "不能为空"));
                }
            } catch (org.springframework.beans.NotReadablePropertyException e) {
                log.warn("类 {} 中无法读取字段 {}", className, rule.getFieldName());
            } catch (Exception e) {
                log.error("读取字段异常: " + rule.getFieldName(), e);
            }
        }

        return errors;
    }

    private String getMessage(FieldRule rule, String defaultMessage) {
        if (StringUtils.hasText(rule.getMessage())) {
            return rule.getMessage();
        }
        return rule.getFieldName() + defaultMessage;
    }
}
