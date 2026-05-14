package com.ruoyi.common.importexport.exception;

/**
 * 导入导出统一异常
 *
 * @author ruoyi
 */
public class ImportExportException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ImportExportException(String message) {
        super(message);
    }

    public ImportExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
