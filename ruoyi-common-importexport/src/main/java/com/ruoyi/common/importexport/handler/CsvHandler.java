package com.ruoyi.common.importexport.handler;

import com.ruoyi.common.importexport.core.AbstractImportExportHandler;
import com.ruoyi.common.importexport.enums.FileTypeEnum;
import com.ruoyi.common.importexport.exception.ImportExportException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV 导入导出处理器 (基于 Apache Commons CSV)
 *
 * @author ruoyi
 */
@Slf4j
@Component
public class CsvHandler<T> extends AbstractImportExportHandler<T> {

    // 添加BOM防止Excel打开乱码
    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    @Override
    public FileTypeEnum getSupportedFileType() {
        return FileTypeEnum.CSV;
    }

    @Override
    protected void doExport(List<T> data, Class<T> clazz, OutputStream os) {
        if (data == null || data.isEmpty()) {
            return;
        }

        try {
            // 写 BOM
            os.write(UTF8_BOM);
            OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);

            Field[] declaredFields = clazz.getDeclaredFields();
            List<Field> fields = new ArrayList<>();
            for (Field f : declaredFields) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers()) && !java.lang.reflect.Modifier.isTransient(f.getModifiers())) {
                    fields.add(f);
                }
            }

            String[] headers = new String[fields.size()];
            for (int i = 0; i < fields.size(); i++) {
                headers[i] = fields.get(i).getName();
            }

            try (CSVPrinter printer = new CSVPrinter(osw, CSVFormat.DEFAULT.withHeader(headers))) {
                for (T item : data) {
                    List<Object> record = new ArrayList<>();
                    for (Field field : fields) {
                        field.setAccessible(true);
                        Object value = field.get(item);
                        if (value != null && !(value instanceof String) && !(value instanceof Number) && !(value instanceof Boolean)) {
                            value = com.alibaba.fastjson2.JSON.toJSONString(value);
                        }
                        record.add(value != null ? value.toString() : "");
                    }
                    printer.printRecord(record);
                }
                printer.flush();
            }
        } catch (Exception e) {
            log.error("CSV导出异常", e);
            throw new ImportExportException("CSV导出异常: " + e.getMessage(), e);
        }
    }

    @Override
    protected void doExportBigData(Iterable<List<T>> dataIterable, Class<T> clazz, OutputStream os) {
        try {
            // 写 BOM
            os.write(UTF8_BOM);
            OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);

            Field[] declaredFields = clazz.getDeclaredFields();
            List<Field> fields = new ArrayList<>();
            for (Field f : declaredFields) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers()) && !java.lang.reflect.Modifier.isTransient(f.getModifiers())) {
                    fields.add(f);
                }
            }

            String[] headers = new String[fields.size()];
            for (int i = 0; i < fields.size(); i++) {
                headers[i] = fields.get(i).getName();
            }

            try (CSVPrinter printer = new CSVPrinter(osw, CSVFormat.DEFAULT.withHeader(headers))) {
                for (List<T> batch : dataIterable) {
                    if (batch != null) {
                        for (T item : batch) {
                            List<Object> record = new ArrayList<>();
                            for (Field field : fields) {
                                field.setAccessible(true);
                                Object value = field.get(item);
                                if (value != null && !(value instanceof String) && !(value instanceof Number) && !(value instanceof Boolean)) {
                                    value = com.alibaba.fastjson2.JSON.toJSONString(value);
                                }
                                record.add(value != null ? value.toString() : "");
                            }
                            printer.printRecord(record);
                        }
                        printer.flush();
                    }
                }
            }
        } catch (Exception e) {
            log.error("CSV大数据分批导出异常", e);
            throw new ImportExportException("CSV大数据分批导出异常: " + e.getMessage(), e);
        }
    }

    @Override
    protected List<T> doImport(InputStream is, Class<T> clazz) {
        List<T> resultList = new ArrayList<>();
        try {
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(isr);
            Field[] declaredFields = clazz.getDeclaredFields();
            List<Field> fields = new ArrayList<>();
            for (Field f : declaredFields) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers()) && !java.lang.reflect.Modifier.isTransient(f.getModifiers())) {
                    fields.add(f);
                }
            }

            for (CSVRecord record : parser) {
                T obj = clazz.getDeclaredConstructor().newInstance();
                for (Field field : fields) {
                    field.setAccessible(true);
                    String headerName = field.getName();
                    if (record.isMapped(headerName)) {
                        String valueStr = record.get(headerName);
                        if (valueStr != null && !valueStr.trim().isEmpty()) {
                            // 简单类型转换，实际项目中可能需要更完善的TypeConverter
                            if (field.getType() == String.class) {
                                field.set(obj, valueStr);
                            } else if (field.getType() == Integer.class || field.getType() == int.class) {
                                field.set(obj, Integer.parseInt(valueStr));
                            } else if (field.getType() == Long.class || field.getType() == long.class) {
                                field.set(obj, Long.parseLong(valueStr));
                            } else if (field.getType() == Double.class || field.getType() == double.class) {
                                field.set(obj, Double.parseDouble(valueStr));
                            } else if (field.getType() == Boolean.class || field.getType() == boolean.class) {
                                field.set(obj, Boolean.parseBoolean(valueStr));
                            } else {
                                field.set(obj, com.alibaba.fastjson2.JSON.parseObject(valueStr, field.getType()));
                            }
                        }
                    }
                }
                resultList.add(obj);
            }
        } catch (Exception e) {
            log.error("CSV导入异常", e);
            throw new ImportExportException("CSV导入异常: " + e.getMessage(), e);
        }
        return resultList;
    }
}
