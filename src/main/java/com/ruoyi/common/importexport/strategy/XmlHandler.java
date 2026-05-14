package com.ruoyi.common.importexport.strategy;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.ruoyi.common.importexport.enums.FileTypeEnum;
import com.ruoyi.common.importexport.handler.AbstractImportExportHandler;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Component
public class XmlHandler<T> extends AbstractImportExportHandler<T> {
    private final XmlMapper xmlMapper = new XmlMapper();

    @Override
    public FileTypeEnum getFileType() {
        return FileTypeEnum.XML;
    }

    @Override
    protected void doExport(List<T> data, OutputStream os) throws Exception {
        xmlMapper.writeValue(os, data);
    }

    @Override
    protected List<T> doImport(InputStream is, Class<T> clazz) throws Exception {
        CollectionType listType = xmlMapper.getTypeFactory().constructCollectionType(List.class, clazz);
        return xmlMapper.readValue(is, listType);
    }
}
