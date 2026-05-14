package com.ruoyi.common.importexport.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.importexport.enums.FileTypeEnum;
import com.ruoyi.common.importexport.handler.AbstractImportExportHandler;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class TxtHandler<T> extends AbstractImportExportHandler<T> {

    private final ObjectMapper objectMapper = new ObjectMapper(); // Simple JSON format for lines

    @Override
    public FileTypeEnum getFileType() {
        return FileTypeEnum.TXT;
    }

    @Override
    protected void doExport(List<T> data, OutputStream os) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            for (T item : data) {
                writer.write(objectMapper.writeValueAsString(item));
                writer.newLine();
            }
        }
    }

    @Override
    protected List<T> doImport(InputStream is, Class<T> clazz) throws Exception {
        List<T> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    list.add(objectMapper.readValue(line, clazz));
                }
            }
        }
        return list;
    }
}
