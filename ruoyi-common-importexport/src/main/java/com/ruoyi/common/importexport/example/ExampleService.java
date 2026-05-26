package com.ruoyi.common.importexport.example;

import com.ruoyi.common.importexport.core.ImportExportHandler;
import com.ruoyi.common.importexport.dto.ImportResult;
import com.ruoyi.common.importexport.enums.FileTypeEnum;
import com.ruoyi.common.importexport.factory.FileHandlerFactory;
import com.ruoyi.common.importexport.validation.DynamicImportValidator;
import com.ruoyi.common.importexport.validation.SpelImportValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import com.ruoyi.common.importexport.handler.ExcelHandler;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private SpelImportValidator spelImportValidator;

    /**
     * 通用导出
     */
    public void exportData(String fileType, List<ExampleDTO> dataList, OutputStream os) {
        ImportExportHandler<ExampleDTO> handler = fileHandlerFactory.getHandler(fileType);
        handler.exportData(dataList, ExampleDTO.class, os);
    }

    /**
     * 通用多Sheet导出
     * <p>
     * 兼容性接口：
     * 如果是EXCEL格式，且Handler为ExcelHandler，则使用多Sheet导出。
     * 对于其他格式，将所有Sheet的数据合并后统一导出。
     * </p>
     */
    public void exportMultiSheetData(String fileType, Map<String, List<ExampleDTO>> sheetDataMap, OutputStream os) {
        ImportExportHandler<ExampleDTO> handler = fileHandlerFactory.getHandler(fileType);

        if (FileTypeEnum.EXCEL.name().equalsIgnoreCase(fileType) && handler instanceof ExcelHandler) {
            ((ExcelHandler<ExampleDTO>) handler).exportMultiSheet(sheetDataMap, ExampleDTO.class, os);
        } else {
            List<ExampleDTO> mergedData = new ArrayList<>();
            for (List<ExampleDTO> sheetData : sheetDataMap.values()) {
                if (sheetData != null) {
                    mergedData.addAll(sheetData);
                }
            }
            handler.exportData(mergedData, ExampleDTO.class, os);
        }
    }

    /**
     * 通用导入 (带校验和错误收集)
     */
    public ImportResult<ExampleDTO> importData(String fileType, InputStream is, String engine) {
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

            // 动态规则校验错误 (按指定引擎执行)
            List<String> dynamicErrors;
            if ("spel".equalsIgnoreCase(engine)) {
                dynamicErrors = spelImportValidator.validate(dto);
            } else {
                dynamicErrors = dynamicImportValidator.validate(dto);
            }

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

    /**
     * 分批导入示例 (适合百万级数据)
     *
     * @param fileType 文件类型
     * @param is 输入流
     * @param batchSize 批次大小
     * @return 导入统计结果 (只包含数量和异常信息，不包含成功列表对象，防止OOM)
     */
    public ImportResult<ExampleDTO> importDataInBatches(String fileType, InputStream is, int batchSize) {
        ImportExportHandler<ExampleDTO> handler = fileHandlerFactory.getHandler(fileType);
        ImportResult<ExampleDTO> result = new ImportResult<>();

        java.util.concurrent.atomic.AtomicInteger totalCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);

        handler.importData(is, ExampleDTO.class, batchSize, batchList -> {
            // 模拟批量插入数据库的操作，这里只做校验和计数
            int currentBatchIndex = totalCount.get();
            for (int i = 0; i < batchList.size(); i++) {
                ExampleDTO dto = batchList.get(i);
                Set<ConstraintViolation<ExampleDTO>> violations = validator.validate(dto);

                if (!violations.isEmpty()) {
                    StringBuilder errorMsg = new StringBuilder();
                    for (ConstraintViolation<ExampleDTO> violation : violations) {
                        errorMsg.append(violation.getMessage()).append("; ");
                    }
                    result.addError("第 " + (currentBatchIndex + i + 1) + " 行数据校验失败: " + errorMsg.toString());
                } else {
                    // 这里可以调用 mybatis-plus 的 saveBatch
                    successCount.incrementAndGet();
                }
            }
            totalCount.addAndGet(batchList.size());
            System.out.println("成功处理批次数据，当前共处理: " + totalCount.get() + " 条");
        });

        // 由于结果对象不保存具体成功数据，这里通过修改提示信息或专门DTO返回，为简单起见，利用错误消息或者另外定义属性
        if (result.getErrorMessages() == null || result.getErrorMessages().isEmpty()) {
            result.addError("全部导入成功，共处理: " + successCount.get() + " 条");
        } else {
            result.addError("部分或全部导入失败，总数: " + totalCount.get() + ", 成功: " + successCount.get() + "。详细错误见其他列表。");
        }

        return result;
    }
}
