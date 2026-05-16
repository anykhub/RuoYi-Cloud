package com.ruoyi.common.importexport.example.mapper;

import lombok.Data;

import java.util.List;

@Data
public class UserDTO {
    private String userId;        // 映射自 id，需转换为 String
    private String name;          // 映射自 userName
    private Integer age;          // 映射自 userAge
    private List<String> configs; // 映射自 jsonConfig，需通过自定义方法转换
    private String createDate;    // 映射自 createTime，需格式化
    private Integer status;       // 映射自 isDeleted，需通过自定义逻辑转换
    private String uniqueCode;    // 无直接映射源，需通过 expression 自动生成
}
