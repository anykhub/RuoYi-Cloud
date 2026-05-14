package com.ruoyi.common.importexport.validator;

import com.ruoyi.common.importexport.dto.ImportResult;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.List;
import java.util.Set;

@Component
public class ImportValidator {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public <T> ImportResult<T> validate(List<T> data) {
        ImportResult<T> result = new ImportResult<>();
        for (int i = 0; i < data.size(); i++) {
            T item = data.get(i);
            Set<ConstraintViolation<T>> violations = validator.validate(item);
            if (violations.isEmpty()) {
                result.addSuccess(item);
            } else {
                StringBuilder errorMsg = new StringBuilder("第" + (i + 1) + "行数据校验失败：");
                for (ConstraintViolation<T> violation : violations) {
                    errorMsg.append(violation.getPropertyPath()).append(" ")
                            .append(violation.getMessage()).append("; ");
                }
                result.addError(errorMsg.toString());
            }
        }
        return result;
    }
}
