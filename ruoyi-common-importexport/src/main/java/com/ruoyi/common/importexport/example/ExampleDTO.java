package com.ruoyi.common.importexport.example;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 示例 DTO
 *
 * @author ruoyi
 */
@Data
public class ExampleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ExcelProperty("ID")
    private Long id;

    @NotBlank(message = "用户名不能为空")
    @ExcelProperty("用户名")
    private String username;

    @NotNull(message = "年龄不能为空")
    @ExcelProperty("年龄")
    private Integer age;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty(value = "详情", converter = ExampleDetailDTOConverter.class)
    private ExampleDetailDTO detail;
}
