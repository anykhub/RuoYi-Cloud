package com.ruoyi.common.importexport.factory;

import com.ruoyi.common.importexport.core.ImportExportHandler;
import com.ruoyi.common.importexport.enums.FileTypeEnum;
import com.ruoyi.common.importexport.exception.ImportExportException;
import com.ruoyi.common.importexport.registry.FileHandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileHandlerFactory {

    @Autowired
    private FileHandlerRegistry registry;

    @SuppressWarnings("unchecked")
    public <T> ImportExportHandler<T> getHandler(FileTypeEnum fileType) {
        ImportExportHandler<?> handler = registry.getHandler(fileType);
        if (handler == null) {
            throw new ImportExportException("不支持的文件类型: " + fileType);
        }
        return (ImportExportHandler<T>) handler;
    }
}
