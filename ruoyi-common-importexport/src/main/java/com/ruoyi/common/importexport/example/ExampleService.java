package com.ruoyi.common.importexport.example;

import com.ruoyi.common.importexport.core.ImportExportHandler;
import com.ruoyi.common.importexport.dto.ImportResult;
import com.ruoyi.common.importexport.enums.FileTypeEnum;
import com.ruoyi.common.importexport.factory.FileHandlerFactory;
import com.ruoyi.common.importexport.validation.DynamicImportValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

/**
 * 示例 Service
 *
 * @author ruoyi
 */
@Service
public class ExampleService {

    @Autowired
    private FileHandlerFactory fileHandlerFactory;

    @Autowired
    private Validator validator;

    @Autowired
    private DynamicImportValidator dynamicImportValidator;

    /**
     * 通用导出
     */
    public void exportData(String fileType, List<ExampleDTO> dataList, OutputStream os) {
        ImportExportHandler<ExampleDTO> handler = fileHandlerFactory.getHandler(fileType);
        handler.exportData(dataList, ExampleDTO.class, os);
    }

    /**
     * 通用导入 (带校验和错误收集)
     */
    public ImportResult<ExampleDTO> importData(String fileType, InputStream is) {
        ImportExportHandler<ExampleDTO> handler = fileHandlerFactory.getHandler(fileType);

        // 1. 获取原始数据
        List<ExampleDTO> rawDataList = handler.importData(is, ExampleDTO.class);

        ImportResult<ExampleDTO> result = new ImportResult<>();

        // 2. 遍历校验
        for (int i = 0; i < rawDataList.size(); i++) {
            ExampleDTO dto = rawDataList.get(i);
            Set<ConstraintViolation<ExampleDTO>> violations = validator.validate(dto);

            // 静态注解校验错误
            StringBuilder errorMsg = new StringBuilder();
            if (!violations.isEmpty()) {
                for (ConstraintViolation<ExampleDTO> violation : violations) {
                    errorMsg.append(violation.getMessage()).append("; ");
                }
            }

            // 动态规则校验错误
            List<String> dynamicErrors = dynamicImportValidator.validate(dto);
            if (!dynamicErrors.isEmpty()) {
                for (String err : dynamicErrors) {
                    errorMsg.append(err).append("; ");
                }
            }

            if (errorMsg.length() == 0) {
                result.addSuccess(dto);
            } else {
                result.addError("第 " + (i + 1) + " 行数据校验失败: " + errorMsg.toString());
            }
        }

        // 3. 处理成功的业务数据...
        // if (!result.getSuccessList().isEmpty()) {
        //     saveBatch(result.getSuccessList());
        // }

        return result;
    }
}
