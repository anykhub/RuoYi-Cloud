package com.ruoyi.common.importexport.validation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.NullValueInNestedPathException;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.io.ClassPathResource;
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

    @Override
    public void afterPropertiesSet() {
        loadRules();
    }

    /**
     * 从类路径下的 import-rules.json 加载规则
     */
    public void loadRules() {
        try {
            ClassPathResource resource = new ClassPathResource("import-rules.json");
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    List<FieldRule> rules = objectMapper.readValue(is, new TypeReference<List<FieldRule>>() {});
                    classRulesMap.clear();
                    for (FieldRule rule : rules) {
                        classRulesMap.computeIfAbsent(rule.getClassName(), k -> new ArrayList<>()).add(rule);
                    }
                    log.info("加载动态校验规则完成, 共 {} 条规则", rules.size());
                }
            } else {
                log.info("未找到动态校验规则配置文件: import-rules.json");
            }
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
