package com.ruoyi.common.importexport.enums;

public enum FileTypeEnum {
    EXCEL("excel"),
    CSV("csv"),
    JSON("json"),
    XML("xml"),
    TXT("txt");

    private final String type;

    FileTypeEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
