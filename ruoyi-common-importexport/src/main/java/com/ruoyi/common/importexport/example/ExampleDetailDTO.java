package com.ruoyi.common.importexport.example;

import lombok.Data;
import java.io.Serializable;

/**
 * 示例嵌套 DTO
 *
 * @author ruoyi
 */
@Data
public class ExampleDetailDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String description;
}
