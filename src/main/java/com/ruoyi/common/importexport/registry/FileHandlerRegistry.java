package com.ruoyi.common.importexport.registry;

import com.ruoyi.common.importexport.core.ImportExportHandler;
import com.ruoyi.common.importexport.enums.FileTypeEnum;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FileHandlerRegistry {
    private final Map<FileTypeEnum, ImportExportHandler<?>> handlerMap = new ConcurrentHashMap<>();

    public void register(FileTypeEnum fileType, ImportExportHandler<?> handler) {
        handlerMap.put(fileType, handler);
    }

    public ImportExportHandler<?> getHandler(FileTypeEnum fileType) {
        return handlerMap.get(fileType);
    }
}
