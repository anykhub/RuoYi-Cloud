package com.ruoyi.common.importexport.handler;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.ruoyi.common.importexport.core.AbstractImportExportHandler;
import com.ruoyi.common.importexport.enums.FileTypeEnum;
import com.ruoyi.common.importexport.exception.ImportExportException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;

/**
 * Excel 导入导出处理器 (基于 EasyExcel)
 *
 * 采用策略模式实现具体的Excel处理策略
 * 支持大数据量流式读写
 *
 * @author ruoyi
 */
@Slf4j
@Component
public class ExcelHandler<T> extends AbstractImportExportHandler<T> {

    @Override
    public FileTypeEnum getSupportedFileType() {
        return FileTypeEnum.EXCEL;
    }

    @Override
    protected void doExport(List<T> data, Class<T> clazz, OutputStream os) {
        ExcelWriter excelWriter = null;
        try {
            excelWriter = EasyExcel.write(os, clazz).build();
            WriteSheet writeSheet = EasyExcel.writerSheet("Sheet1").build();

            // 分批写入并释放原列表内存，防止百万数据OOM
            java.util.ListIterator<T> iterator = data.listIterator();
            List<T> batch = new ArrayList<>(5000);
            while (iterator.hasNext()) {
                batch.add(iterator.next());
                try {
                    iterator.set(null);
                } catch (UnsupportedOperationException e) {
                    // 如果传入的是不可变集合，忽略异常
                }

                if (batch.size() >= 5000 || !iterator.hasNext()) {
                    excelWriter.write(batch, writeSheet);
                    batch.clear();
                }
            }
        } catch (Exception e) {
            log.error("Excel导出异常", e);
            throw new ImportExportException("Excel导出异常: " + e.getMessage(), e);
        } finally {
            if (excelWriter != null) {
                excelWriter.finish();
            }
        }
    }

    /**
     * 多Sheet导出
     *
     * @param sheetDataMap 每个Sheet的名称和对应的数据列表
     * @param clazz 目标类
     * @param os 输出流
     */
    public void exportMultiSheet(Map<String, List<T>> sheetDataMap, Class<T> clazz, OutputStream os) {
        if (sheetDataMap == null || sheetDataMap.isEmpty()) {
            throw new IllegalArgumentException("导出数据不能为空");
        }
        ExcelWriter excelWriter = null;
        try {
            excelWriter = EasyExcel.write(os, clazz).build();
            int sheetNo = 0;
            for (Map.Entry<String, List<T>> entry : sheetDataMap.entrySet()) {
                String sheetName = entry.getKey();
                List<T> data = entry.getValue();
                WriteSheet writeSheet = EasyExcel.writerSheet(sheetNo++, sheetName).build();

                if (data != null) {
                    java.util.ListIterator<T> iterator = data.listIterator();
                    List<T> batch = new ArrayList<>(5000);
                    while (iterator.hasNext()) {
                        batch.add(iterator.next());
                        try {
                            iterator.set(null);
                        } catch (UnsupportedOperationException e) {
                            // 忽略不可变集合异常
                        }

                        if (batch.size() >= 5000 || !iterator.hasNext()) {
                            excelWriter.write(batch, writeSheet);
                            batch.clear();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Excel多Sheet导出异常", e);
            throw new ImportExportException("Excel多Sheet导出异常: " + e.getMessage(), e);
        } finally {
            if (excelWriter != null) {
                excelWriter.finish();
            }
        }
    }

    @Override
    protected List<T> doImport(InputStream is, Class<T> clazz) {
        List<T> resultList = new ArrayList<>();
        try {
            // 使用 EasyExcel 流式读取，防止 OOM
            EasyExcel.read(is, clazz, new ReadListener<T>() {
                @Override
                public void invoke(T data, AnalysisContext context) {
                    resultList.add(data);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("Excel读取完成，共解析 {} 条数据", resultList.size());
                }
            }).sheet().doRead();
        } catch (Exception e) {
            log.error("Excel导入异常", e);
            throw new ImportExportException("Excel导入异常: " + e.getMessage(), e);
        }
        return resultList;
    }
}
