package com.ruoyi.common.importexport.handler;

import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ruoyi.common.importexport.core.AbstractImportExportHandler;
import com.ruoyi.common.importexport.enums.FileTypeEnum;
import com.ruoyi.common.importexport.exception.ImportExportException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * XML 导入导出处理器 (基于 Jackson XML)
 *
 * @author ruoyi
 */
@Slf4j
@Component
public class XmlHandler<T> extends AbstractImportExportHandler<T> {

    private final XmlMapper xmlMapper = new XmlMapper();

    @Override
    public FileTypeEnum getSupportedFileType() {
        return FileTypeEnum.XML;
    }

    @Override
    protected void doExport(List<T> data, Class<T> clazz, OutputStream os) {
        try (com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator xmlGen = (com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator) xmlMapper.getFactory().createGenerator(os)) {
            xmlGen.useDefaultPrettyPrinter();
            xmlGen.setNextName(new javax.xml.namespace.QName("ArrayList"));
            xmlGen.writeStartObject();
            java.util.ListIterator<T> iterator = data.listIterator();
            while (iterator.hasNext()) {
                T item = iterator.next();
                xmlGen.writeFieldName("item");
                xmlMapper.writeValue(xmlGen, item);
                try {
                    iterator.set(null); // 释放内存，防止百万数据OOM
                } catch (UnsupportedOperationException e) {
                    // 如果传入的是不可变集合，忽略异常
                }
            }
            xmlGen.writeEndObject();
        } catch (Exception e) {
            log.error("XML导出异常", e);
            throw new ImportExportException("XML导出异常: " + e.getMessage(), e);
        }
    }

    @Override
    protected List<T> doImport(InputStream is, Class<T> clazz) {
        try {
            CollectionType listType = xmlMapper.getTypeFactory().constructCollectionType(List.class, clazz);
            return xmlMapper.readValue(is, listType);
        } catch (Exception e) {
            log.error("XML导入异常", e);
            throw new ImportExportException("XML导入异常: " + e.getMessage(), e);
        }
    }
}
