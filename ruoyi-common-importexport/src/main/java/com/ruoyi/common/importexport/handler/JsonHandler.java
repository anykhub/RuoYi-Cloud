package com.ruoyi.common.importexport.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.ruoyi.common.importexport.core.AbstractImportExportHandler;
import com.ruoyi.common.importexport.enums.FileTypeEnum;
import com.ruoyi.common.importexport.exception.ImportExportException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * JSON 导入导出处理器 (基于 Jackson)
 *
 * @author ruoyi
 */
@Slf4j
@Component
public class JsonHandler<T> extends AbstractImportExportHandler<T> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public FileTypeEnum getSupportedFileType() {
        return FileTypeEnum.JSON;
    }

    @Override
    protected void doExport(List<T> data, Class<T> clazz, OutputStream os) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(os, data);
        } catch (Exception e) {
            log.error("JSON导出异常", e);
            throw new ImportExportException("JSON导出异常: " + e.getMessage(), e);
        }
    }

    @Override
    protected void doExportBigData(Iterable<List<T>> dataIterable, Class<T> clazz, OutputStream os) {
        try (SequenceWriter sw = objectMapper.writerWithDefaultPrettyPrinter().writeValuesAsArray(os)) {
            for (List<T> batch : dataIterable) {
                if (batch != null) {
                    for (T item : batch) {
                        sw.write(item);
                    }
                }
            }
        } catch (Exception e) {
            log.error("JSON大数据分批导出异常", e);
            throw new ImportExportException("JSON大数据分批导出异常: " + e.getMessage(), e);
        }
    }

    @Override
    protected List<T> doImport(InputStream is, Class<T> clazz) {
        try {
            CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
            return objectMapper.readValue(is, listType);
        } catch (Exception e) {
            log.error("JSON导入异常", e);
            throw new ImportExportException("JSON导入异常: " + e.getMessage(), e);
        }
    }
}
