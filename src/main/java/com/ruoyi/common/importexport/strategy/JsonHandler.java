package com.ruoyi.common.importexport.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.ruoyi.common.importexport.enums.FileTypeEnum;
import com.ruoyi.common.importexport.handler.AbstractImportExportHandler;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Component
public class JsonHandler<T> extends AbstractImportExportHandler<T> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public FileTypeEnum getFileType() {
        return FileTypeEnum.JSON;
    }

    @Override
    protected void doExport(List<T> data, OutputStream os) throws Exception {
        objectMapper.writeValue(os, data);
    }

    @Override
    protected List<T> doImport(InputStream is, Class<T> clazz) throws Exception {
        CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
        return objectMapper.readValue(is, listType);
    }
}
