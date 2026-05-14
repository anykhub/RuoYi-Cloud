package com.ruoyi.common.importexport.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ImportResult<T> {
    private List<T> successList = new ArrayList<>();
    private List<String> errorMessages = new ArrayList<>();

    public void addSuccess(T data) {
        successList.add(data);
    }

    public void addError(String message) {
        errorMessages.add(message);
    }
}
