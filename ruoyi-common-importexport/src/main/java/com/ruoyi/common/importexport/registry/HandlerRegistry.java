package com.ruoyi.common.importexport.registry;

import com.ruoyi.common.importexport.core.ImportExportHandler;
import com.ruoyi.common.importexport.enums.FileTypeEnum;
import com.ruoyi.common.importexport.exception.ImportExportException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 导入导出处理器注册中心
 *
 * 采用注册中心模式，统一管理所有的Handler
 *
 * @author ruoyi
 */
@Component
public class HandlerRegistry {

    /**
     * 存储处理器，Key为文件类型，Value为对应的处理器
     */
    private final Map<FileTypeEnum, ImportExportHandler<?>> handlerMap = new ConcurrentHashMap<>();

    /**
     * 注册处理器
     *
     * @param handler 处理器
     */
    public void register(ImportExportHandler<?> handler) {
        if (handler == null) {
            return;
        }
        FileTypeEnum fileType = handler.getSupportedFileType();
        if (fileType != null) {
            handlerMap.put(fileType, handler);
        }
    }

    /**
     * 获取处理器
     *
     * @param fileType 文件类型
     * @param <T> 实体类型泛型
     * @return 对应的处理器
     */
    @SuppressWarnings("unchecked")
    public <T> ImportExportHandler<T> getHandler(FileTypeEnum fileType) {
        ImportExportHandler<?> handler = handlerMap.get(fileType);
        if (handler == null) {
            throw new ImportExportException("不支持的文件类型: " + (fileType != null ? fileType.getCode() : "null"));
        }
        return (ImportExportHandler<T>) handler;
    }
}
