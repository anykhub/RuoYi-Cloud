package com.ruoyi.common.importexport.validation;

import lombok.Data;

/**
 * 字段校验规则模型
 */
@Data
public class FieldRule {

    /**
     * 类全限定名
     */
    private String className;

    /**
     * 字段名
     */
    private String fieldName;

    /**
     * 是否必填
     */
    private Boolean required;

    /**
     * 正则表达式
     */
    private String regex;

    /**
     * 错误提示信息
     */
    private String message;
}
