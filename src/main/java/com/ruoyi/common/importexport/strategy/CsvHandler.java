package com.ruoyi.common.importexport.strategy;

import com.ruoyi.common.importexport.enums.FileTypeEnum;
import com.ruoyi.common.importexport.handler.AbstractImportExportHandler;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.CSVParser;
import org.springframework.stereotype.Component;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvHandler<T> extends AbstractImportExportHandler<T> {

    @Override
    public FileTypeEnum getFileType() {
        return FileTypeEnum.CSV;
    }

    @Override
    protected void doExport(List<T> data, OutputStream os) throws Exception {
        if (data == null || data.isEmpty()) {
            return;
        }
        Class<?> clazz = data.get(0).getClass();
        Field[] fields = clazz.getDeclaredFields();
        List<String> headers = new ArrayList<>();
        for (Field field : fields) {
            headers.add(field.getName());
        }

        // Write UTF-8 BOM before wrapping in Writers
        os.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        try (OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(osw, CSVFormat.DEFAULT.withHeader(headers.toArray(new String[0])))) {
            for (T item : data) {
                List<Object> values = new ArrayList<>();
                for (Field field : fields) {
                    field.setAccessible(true);
                    Object val = field.get(item);
                    values.add(val != null ? val.toString() : "");
                }
                printer.printRecord(values);
            }
        }
    }

    @Override
    protected List<T> doImport(InputStream is, Class<T> clazz) throws Exception {
        List<T> list = new ArrayList<>();
        Field[] fields = clazz.getDeclaredFields();
        try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(isr)) {
            for (CSVRecord record : parser) {
                T obj = clazz.getDeclaredConstructor().newInstance();
                for (Field field : fields) {
                    if (record.isMapped(field.getName())) {
                        field.setAccessible(true);
                        String val = record.get(field.getName());
                        // Simple conversion (can be enhanced)
                        if (field.getType() == String.class) {
                            field.set(obj, val);
                        } else if (field.getType() == Integer.class || field.getType() == int.class) {
                            field.set(obj, Integer.parseInt(val));
                        }
                    }
                }
                list.add(obj);
            }
        }
        return list;
    }
}
