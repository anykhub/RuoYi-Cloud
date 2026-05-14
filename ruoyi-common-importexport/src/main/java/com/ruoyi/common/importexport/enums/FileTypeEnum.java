package com.ruoyi.common.importexport.enums;

/**
 * 文件类型枚举
 *
 * @author ruoyi
 */
public enum FileTypeEnum {

    /**
     * Excel 文件 (.xls, .xlsx)
     */
    EXCEL("excel", "Excel文件"),

    /**
     * CSV 文件 (.csv)
     */
    CSV("csv", "CSV文件"),

    /**
     * JSON 文件 (.json)
     */
    JSON("json", "JSON文件"),

    /**
     * XML 文件 (.xml)
     */
    XML("xml", "XML文件"),

    /**
     * TXT 文件 (.txt)
     */
    TXT("txt", "TXT文件");

    private final String code;
    private final String desc;

    FileTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据code获取枚举
     * @param code 代码
     * @return 对应的文件类型枚举，找不到返回null
     */
    public static FileTypeEnum getByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        for (FileTypeEnum type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
