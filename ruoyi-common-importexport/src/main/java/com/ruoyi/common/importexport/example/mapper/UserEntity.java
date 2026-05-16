package com.ruoyi.common.importexport.example.mapper;

import lombok.Data;

import java.util.Date;

@Data
public class UserEntity {
    private Long id;
    private String userName;
    private Integer userAge;
    private String jsonConfig;
    private Date createTime;
    private Boolean isDeleted;
}
