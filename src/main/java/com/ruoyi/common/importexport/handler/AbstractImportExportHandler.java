package com.ruoyi.common.importexport.handler;

import com.ruoyi.common.importexport.core.ImportExportHandler;
import com.ruoyi.common.importexport.enums.FileTypeEnum;
import com.ruoyi.common.importexport.exception.ImportExportException;
import com.ruoyi.common.importexport.registry.FileHandlerRegistry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public abstract class AbstractImportExportHandler<T> implements ImportExportHandler<T>, InitializingBean {

    @Autowired
    private FileHandlerRegistry fileHandlerRegistry;

    @Override
    public void exportData(List<T> data, OutputStream os) {
        try {
            doExport(data, os);
        } catch (Exception e) {
            throw new ImportExportException("导出数据失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<T> importData(InputStream is, Class<T> clazz) {
        try {
            return doImport(is, clazz);
        } catch (Exception e) {
            throw new ImportExportException("导入数据失败: " + e.getMessage(), e);
        }
    }

    protected abstract void doExport(List<T> data, OutputStream os) throws Exception;

    protected abstract List<T> doImport(InputStream is, Class<T> clazz) throws Exception;

    @Override
    public void afterPropertiesSet() throws Exception {
        fileHandlerRegistry.register(getFileType(), this);
    }
}
