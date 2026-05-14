package com.ruoyi.common.importexport.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 导入结果 DTO
 *
 * @author ruoyi
 * @param <T> 数据类型泛型
 */
@Data
public class ImportResult<T> {

    /**
     * 成功的数据列表
     */
    private List<T> successList = new ArrayList<>();

    /**
     * 失败的错误信息列表
     */
    private List<String> errorMessages = new ArrayList<>();

    /**
     * 是否完全成功
     *
     * @return boolean
     */
    public boolean isSuccess() {
        return errorMessages.isEmpty();
    }

    /**
     * 添加成功数据
     */
    public void addSuccess(T data) {
        this.successList.add(data);
    }

    /**
     * 添加错误信息
     */
    public void addError(String message) {
        this.errorMessages.add(message);
    }
}
