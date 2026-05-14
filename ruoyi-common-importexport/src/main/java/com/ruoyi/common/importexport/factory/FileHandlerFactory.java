package com.ruoyi.common.importexport.factory;

import com.ruoyi.common.importexport.core.ImportExportHandler;
import com.ruoyi.common.importexport.enums.FileTypeEnum;
import com.ruoyi.common.importexport.registry.HandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 导入导出处理器工厂类
 *
 * 使用工厂模式对外界屏蔽注册中心的具体实现，提供统一获取Handler的入口
 * 避免大量的 if else 分支判断
 *
 * @author ruoyi
 */
@Component
public class FileHandlerFactory {

    private final HandlerRegistry registry;

    @Autowired
    public FileHandlerFactory(HandlerRegistry registry) {
        this.registry = registry;
    }

    /**
     * 根据文件类型枚举获取对应的处理器
     *
     * @param fileType 文件类型枚举
     * @param <T> 数据泛型
     * @return 对应的导入导出处理器
     */
    public <T> ImportExportHandler<T> getHandler(FileTypeEnum fileType) {
        return registry.getHandler(fileType);
    }

    /**
     * 根据文件类型字符串(code)获取对应的处理器
     *
     * @param typeCode 文件类型字符串 (如 "excel", "csv")
     * @param <T> 数据泛型
     * @return 对应的导入导出处理器
     */
    public <T> ImportExportHandler<T> getHandler(String typeCode) {
        FileTypeEnum fileType = FileTypeEnum.getByCode(typeCode);
        return registry.getHandler(fileType);
    }
}
