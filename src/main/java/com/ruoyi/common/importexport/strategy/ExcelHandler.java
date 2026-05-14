package com.ruoyi.common.importexport.strategy;

import com.alibaba.excel.EasyExcel;
import com.ruoyi.common.importexport.enums.FileTypeEnum;
import com.ruoyi.common.importexport.handler.AbstractImportExportHandler;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Component
public class ExcelHandler<T> extends AbstractImportExportHandler<T> {

    @Override
    public FileTypeEnum getFileType() {
        return FileTypeEnum.EXCEL;
    }

    @Override
    protected void doExport(List<T> data, OutputStream os) throws Exception {
        if (data == null || data.isEmpty()) {
            return;
        }
        Class<?> clazz = data.get(0).getClass();
        EasyExcel.write(os, clazz).sheet("Sheet1").doWrite(data);
    }

    @Override
    protected List<T> doImport(InputStream is, Class<T> clazz) throws Exception {
        return EasyExcel.read(is).head(clazz).sheet().doReadSync();
    }
}
