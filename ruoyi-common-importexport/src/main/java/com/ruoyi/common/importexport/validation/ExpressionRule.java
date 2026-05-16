package com.ruoyi.common.importexport.validation;

import lombok.Data;

/**
 * 基于表达式引擎的字段校验规则
 */
@Data
public class ExpressionRule {

    /**
     * 需要校验的类名全路径
     */
    private String className;

    /**
     * 错误提示字段名 (可选，用于展示)
     */
    private String fieldName;

    /**
     * 执行表达式，返回 true 表示校验通过，false 表示失败
     * 例如：age >= 0 && age <= 199
     */
    private String expression;

    /**
     * 校验失败时的提示信息
     */
    private String message;
}
