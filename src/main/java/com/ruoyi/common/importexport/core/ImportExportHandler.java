package com.ruoyi.common.importexport.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import com.ruoyi.common.importexport.enums.FileTypeEnum;

public interface ImportExportHandler<T> {
    void exportData(List<T> data, OutputStream os);
    List<T> importData(InputStream is, Class<T> clazz);
    FileTypeEnum getFileType();
}
