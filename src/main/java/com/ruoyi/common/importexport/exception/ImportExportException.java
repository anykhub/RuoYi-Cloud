package com.ruoyi.common.importexport.exception;

public class ImportExportException extends RuntimeException {
    public ImportExportException(String message) {
        super(message);
    }
    public ImportExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
