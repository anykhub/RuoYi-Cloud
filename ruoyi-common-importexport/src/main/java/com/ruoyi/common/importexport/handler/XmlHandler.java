package com.ruoyi.common.importexport.handler;

import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.ruoyi.common.importexport.core.AbstractImportExportHandler;
import com.ruoyi.common.importexport.enums.FileTypeEnum;
import com.ruoyi.common.importexport.exception.ImportExportException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
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
        try {
            xmlMapper.writerWithDefaultPrettyPrinter().writeValue(os, data);
        } catch (Exception e) {
            log.error("XML导出异常", e);
            throw new ImportExportException("XML导出异常: " + e.getMessage(), e);
        }
    }

    @Override
    protected void doExportBigData(Iterable<List<T>> dataIterable, Class<T> clazz, OutputStream os) {
        try (ToXmlGenerator xg = (ToXmlGenerator) xmlMapper.getFactory().createGenerator(os)) {
            xg.setNextName(new QName("ArrayList"));
            xg.writeStartObject();
            for (List<T> batch : dataIterable) {
                if (batch != null) {
                    for (T item : batch) {
                        xg.setNextName(new QName("item"));
                        xg.writeFieldName("item");
                        xmlMapper.writeValue(xg, item);
                    }
                }
            }
            xg.writeEndObject();
        } catch (Exception e) {
            log.error("XML大数据分批导出异常", e);
            throw new ImportExportException("XML大数据分批导出异常: " + e.getMessage(), e);
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
