package com.ruoyi.demo.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class UserDTO {
    @ExcelProperty("用户名")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @ExcelProperty("年龄")
    @NotNull(message = "年龄不能为空")
    private Integer age;
}
