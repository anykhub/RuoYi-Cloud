package com.ruoyi.common.importexport.validation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 Spring Expression Language (SpEL) 的轻量级表达式校验引擎
 * 适用于跨字段复杂校验场景
 */
@Slf4j
@Component
public class SpelImportValidator implements InitializingBean {

    private final Map<String, List<ExpressionRule>> classRulesMap = new HashMap<>();

    // 缓存解析后的表达式，提升执行性能
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExpressionParser parser = new SpelExpressionParser();

    @Value("${importexport.spel-rules.location:classpath*:import-spel-rules.json}")
    private String rulesLocation;

    @Value("${importexport.spel-rules.content:}")
    private String rulesContent;

    @Value("${importexport.spel-rules.nacos-data-id:import-spel-rules.json}")
    private String nacosDataId;

    @Autowired
    private Environment environment;

    @Override
    public void afterPropertiesSet() {
        loadRules();
    }

    /**
     * 加载 SpEL 校验规则 (优先从直接配置的内容读取，否则从 Nacos DataID 读取，最后从文件路径读取)
     */
    public void loadRules() {
        classRulesMap.clear();
        expressionCache.clear();
        int totalRules = 0;

        try {
            // 1. 优先解析直接注入的 JSON 内容
            if (StringUtils.hasText(rulesContent)) {
                List<ExpressionRule> rules = objectMapper.readValue(rulesContent, new TypeReference<List<ExpressionRule>>() {});
                for (ExpressionRule rule : rules) {
                    classRulesMap.computeIfAbsent(rule.getClassName(), k -> new ArrayList<>()).add(rule);
                    totalRules++;
                }
                log.info("从直接配置的内容 (importexport.spel-rules.content) 加载 SpEL 校验规则完成, 共 {} 条规则", totalRules);
                return;
            }

            // 2. 尝试解析从 Nacos 直接挂载为文件的独立配置
            if (StringUtils.hasText(nacosDataId)) {
                String nacosContent = environment.getProperty(nacosDataId);
                if (StringUtils.hasText(nacosContent)) {
                    List<ExpressionRule> rules = objectMapper.readValue(nacosContent, new TypeReference<List<ExpressionRule>>() {});
                    for (ExpressionRule rule : rules) {
                        classRulesMap.computeIfAbsent(rule.getClassName(), k -> new ArrayList<>()).add(rule);
                        totalRules++;
                    }
                    log.info("从 Nacos 独立文件配置 ({}) 加载 SpEL 校验规则完成, 共 {} 条规则", nacosDataId, totalRules);
                    return;
                }
            }

            // 3. 最后退化到通过路径解析
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(rulesLocation);

            if (resources.length == 0) {
                log.info("未找到 SpEL 校验规则配置文件: {}", rulesLocation);
                return;
            }

            for (Resource resource : resources) {
                if (resource.exists()) {
                    try (InputStream is = resource.getInputStream()) {
                        List<ExpressionRule> rules = objectMapper.readValue(is, new TypeReference<List<ExpressionRule>>() {});
                        for (ExpressionRule rule : rules) {
                            classRulesMap.computeIfAbsent(rule.getClassName(), k -> new ArrayList<>()).add(rule);
                            totalRules++;
                        }
                    } catch (Exception ex) {
                        log.error("解析 SpEL 规则文件失败: " + resource.getFilename(), ex);
                    }
                }
            }
            log.info("从 {} 个文件加载 SpEL 校验规则完成, 共 {} 条规则", resources.length, totalRules);
        } catch (Exception e) {
            log.error("加载 SpEL 校验规则失败", e);
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
        List<ExpressionRule> rules = classRulesMap.get(className);

        if (rules == null || rules.isEmpty()) {
            return errors;
        }

        EvaluationContext context = new StandardEvaluationContext(target);

        for (ExpressionRule rule : rules) {
            try {
                Expression expression = expressionCache.computeIfAbsent(rule.getExpression(), parser::parseExpression);
                Boolean result = expression.getValue(context, Boolean.class);

                // 如果表达式执行结果不为 true，则认为校验失败
                if (result == null || !result) {
                    errors.add(getMessage(rule, "校验未通过"));
                }
            } catch (Exception e) {
                log.error("执行 SpEL 表达式异常: " + rule.getExpression(), e);
                errors.add(getMessage(rule, "表达式执行异常"));
            }
        }

        return errors;
    }

    private String getMessage(ExpressionRule rule, String defaultMessage) {
        if (StringUtils.hasText(rule.getMessage())) {
            return rule.getMessage();
        }
        return (StringUtils.hasText(rule.getFieldName()) ? rule.getFieldName() : rule.getExpression()) + defaultMessage;
    }
}
