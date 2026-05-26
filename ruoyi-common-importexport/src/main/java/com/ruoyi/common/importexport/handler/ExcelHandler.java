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
        try {
            // EasyExcel 默认即为流式导出，防止 OOM
            EasyExcel.write(os, clazz).sheet("Sheet1").doWrite(data);
        } catch (Exception e) {
            log.error("Excel导出异常", e);
            throw new ImportExportException("Excel导出异常: " + e.getMessage(), e);
        }
    }

    @Override
    protected void doExportBigData(Iterable<List<T>> dataIterable, Class<T> clazz, OutputStream os) {
        ExcelWriter excelWriter = null;
        try {
            excelWriter = EasyExcel.write(os, clazz).build();
            WriteSheet writeSheet = EasyExcel.writerSheet("Sheet1").build();
            for (List<T> batch : dataIterable) {
                if (batch != null && !batch.isEmpty()) {
                    excelWriter.write(batch, writeSheet);
                }
            }
        } catch (Exception e) {
            log.error("Excel大数据分批导出异常", e);
            throw new ImportExportException("Excel大数据分批导出异常: " + e.getMessage(), e);
        } finally {
            if (excelWriter != null) {
                excelWriter.finish();
            }
        }
    }

    /**
     * 导出数据，超过最大行数分多个Sheet
     *
     * @param data 要导出的数据
     * @param clazz 目标类
     * @param os 输出流
     * @param maxRowsPerSheet 每个Sheet的最大行数
     */
    public void exportSplitSheet(List<T> data, Class<T> clazz, OutputStream os, int maxRowsPerSheet) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("导出数据不能为空");
        }
        if (maxRowsPerSheet <= 0) {
            throw new IllegalArgumentException("每页最大行数必须大于0");
        }
        ExcelWriter excelWriter = null;
        try {
            excelWriter = EasyExcel.write(os, clazz).build();
            int totalSize = data.size();
            int sheetNo = 0;
            for (int i = 0; i < totalSize; i += maxRowsPerSheet) {
                int toIndex = Math.min(i + maxRowsPerSheet, totalSize);
                List<T> subList = data.subList(i, toIndex);
                WriteSheet writeSheet = EasyExcel.writerSheet(sheetNo, "Sheet" + (sheetNo + 1)).build();
                excelWriter.write(subList, writeSheet);
                sheetNo++;
            }
        } catch (Exception e) {
            log.error("Excel分Sheet导出异常", e);
            throw new ImportExportException("Excel分Sheet导出异常: " + e.getMessage(), e);
        } finally {
            if (excelWriter != null) {
                excelWriter.finish();
            }
        }
    }

    /**
     * 大数据导出，基于函数式接口分页查询，超过最大行数分多个Sheet
     *
     * @param pageDataLoader 分页查询函数 (输入页码，返回该页数据列表。返回空列表或null时停止)
     * @param clazz 目标类
     * @param os 输出流
     * @param maxRowsPerSheet 每个Sheet的最大行数
     */
    public void exportBigDataSplitSheet(java.util.function.Function<Integer, List<T>> pageDataLoader, Class<T> clazz, OutputStream os, int maxRowsPerSheet) {
        if (pageDataLoader == null) {
            throw new IllegalArgumentException("分页查询函数不能为空");
        }

        Iterable<List<T>> iterable = () -> new java.util.Iterator<List<T>>() {
            private int pageNum = 1;
            private List<T> nextBatch = null;
            private boolean isFinished = false;

            @Override
            public boolean hasNext() {
                if (isFinished) {
                    return false;
                }
                if (nextBatch != null) {
                    return true;
                }
                nextBatch = pageDataLoader.apply(pageNum++);
                if (nextBatch == null || nextBatch.isEmpty()) {
                    isFinished = true;
                    return false;
                }
                return true;
            }

            @Override
            public List<T> next() {
                if (!hasNext()) {
                    throw new java.util.NoSuchElementException();
                }
                List<T> result = nextBatch;
                nextBatch = null;
                return result;
            }
        };

        exportBigDataSplitSheet(iterable, clazz, os, maxRowsPerSheet);
    }

    /**
     * 大数据导出，超过最大行数分多个Sheet
     *
     * @param dataIterable 批次数据迭代器
     * @param clazz 目标类
     * @param os 输出流
     * @param maxRowsPerSheet 每个Sheet的最大行数
     */
    public void exportBigDataSplitSheet(Iterable<List<T>> dataIterable, Class<T> clazz, OutputStream os, int maxRowsPerSheet) {
        if (dataIterable == null) {
            throw new IllegalArgumentException("导出数据迭代器不能为空");
        }
        if (maxRowsPerSheet <= 0) {
            throw new IllegalArgumentException("每页最大行数必须大于0");
        }
        ExcelWriter excelWriter = null;
        try {
            excelWriter = EasyExcel.write(os, clazz).build();
            int sheetNo = 0;
            int currentRowCount = 0;
            WriteSheet writeSheet = EasyExcel.writerSheet(sheetNo, "Sheet" + (sheetNo + 1)).build();

            for (List<T> batch : dataIterable) {
                if (batch != null && !batch.isEmpty()) {
                    int batchSize = batch.size();
                    int batchIndex = 0;

                    while (batchIndex < batchSize) {
                        int remainingCapacity = maxRowsPerSheet - currentRowCount;
                        int elementsToWrite = Math.min(remainingCapacity, batchSize - batchIndex);

                        List<T> subList = batch.subList(batchIndex, batchIndex + elementsToWrite);
                        excelWriter.write(subList, writeSheet);

                        currentRowCount += elementsToWrite;
                        batchIndex += elementsToWrite;

                        if (currentRowCount >= maxRowsPerSheet) {
                            sheetNo++;
                            writeSheet = EasyExcel.writerSheet(sheetNo, "Sheet" + (sheetNo + 1)).build();
                            currentRowCount = 0;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Excel大数据分Sheet导出异常", e);
            throw new ImportExportException("Excel大数据分Sheet导出异常: " + e.getMessage(), e);
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
                excelWriter.write(data, writeSheet);
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
            // 使用 EasyExcel 流式读取，支持读取所有Sheet，防止 OOM
            EasyExcel.read(is, clazz, new ReadListener<T>() {
                @Override
                public void invoke(T data, AnalysisContext context) {
                    resultList.add(data);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("Excel读取完成，当前Sheet解析完成");
                }
            }).doReadAll();
            log.info("Excel所有Sheet读取完成，共解析 {} 条数据", resultList.size());
        } catch (Exception e) {
            log.error("Excel导入异常", e);
            throw new ImportExportException("Excel导入异常: " + e.getMessage(), e);
        }
        return resultList;
    }
}
