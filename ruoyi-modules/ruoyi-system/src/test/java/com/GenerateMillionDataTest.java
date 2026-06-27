package com;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 快速生成百万级测试数据工具 (动态字段、自适应大小控制、智能数值型自增)
 */
public class GenerateMillionDataTest {

    // 真实国家列表
    private static final String[] REAL_COUNTRIES = {
        "中国", "美国", "英国", "法国", "德国", "日本", "韩国", "加拿大", "澳大利亚", "俄罗斯",
        "巴西", "印度", "新加坡", "意大利", "西班牙", "荷兰", "瑞士", "瑞典", "新西兰", "南非",
        "墨西哥", "越南", "泰国", "马来西亚", "菲律宾", "印度尼西亚", "阿联酋", "沙特阿拉伯", "土耳其", "埃及",
        "阿根廷", "智利", "哥联比亚", "秘鲁", "爱尔兰", "比利时", "奥地利", "丹麦", "挪威", "芬兰",
        "波兰", "希腊", "葡萄牙", "捷克", "匈牙利", "罗马尼亚", "哈萨克斯坦", "以色列", "巴基斯坦", "孟加拉国"
    };

    private boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        String cleanStr = str.trim();
        return cleanStr.matches("\\d+(\\.0+)?");
    }

    private long parseLongRobust(String str) {
        String cleanStr = str.trim();
        if (cleanStr.contains(".")) {
            cleanStr = cleanStr.substring(0, cleanStr.indexOf("."));
        }
        return Long.parseLong(cleanStr);
    }

    /**
     * 核心字段生成逻辑：根据字段配置的分组大小计算出对应行的值
     */
    private String generateValue(String colName, String seedVal, long globalRowIndex, 
                                 Map<String, Integer> fieldGroupSizes, Map<Long, String> countryCache) {
        Integer groupSize = fieldGroupSizes.get(colName);
        if (groupSize == null) {
            // 没有配置分组大小的字段，直接返回种子值
            return seedVal;
        }

        long groupIndex = globalRowIndex / groupSize;

        if ("country".equals(colName)) {
            return countryCache.computeIfAbsent(groupIndex, k -> 
                REAL_COUNTRIES[ThreadLocalRandom.current().nextInt(REAL_COUNTRIES.length)]
            );
        }

        if (isNumeric(seedVal)) {
            long baseNum = parseLongRobust(seedVal);
            return String.valueOf(baseNum + groupIndex);
        } else {
            if (groupSize == 1) {
                // 如果分组大小为 1，即逐行自增，保留原来的格式下划线+行号
                return seedVal + "_" + (globalRowIndex + 1);
            } else {
                return seedVal + (groupIndex > 0 ? String.valueOf(groupIndex) : "");
            }
        }
    }

    @Test
    public void generateData() throws Exception {
        String inputPath = "d:/fengyong/RuoYi-Cloud-3.6.4/bd.xlsx";
        String outputPath = "d:/fengyong/RuoYi-Cloud-3.6.4/bd_million_java_100m.csv";

        // 配置：各个字段的分组大小（单位：行）。未配置的字段将直接使用种子数据不变。
        // - 如果设置为 1：表示该字段逐行自增。
        // - 如果设置为 N (N > 1)：表示该字段每 N 行换一个值（分组）。
        // - 未配置：该字段始终使用 Excel 的种子行原始值。
        Map<String, Integer> fieldGroupSizes = new HashMap<>();
        fieldGroupSizes.put("bdnm", 10);
        fieldGroupSizes.put("bdfh", 10);
        fieldGroupSizes.put("country", 2000);

        List<String> headers = new ArrayList<>();
        List<String> seedRow = new ArrayList<>();

        File file = new File(inputPath);
        if (file.exists()) {
            try (InputStream is = new FileInputStream(file);
                 Workbook workbook = WorkbookFactory.create(is)) {
                Sheet sheet = workbook.getSheetAt(0);
                
                // 读取全部表头 (第0行)
                Row headerRow = sheet.getRow(0);
                if (headerRow != null) {
                    for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                        Cell cell = headerRow.getCell(c);
                        headers.add(cell != null ? cell.toString().trim() : "");
                    }
                }
                
                // 读取第一行作为种子数据 (第1行)
                Row dataRow = sheet.getRow(1);
                if (dataRow != null) {
                    for (int c = 0; c < headers.size(); c++) {
                        Cell cell = dataRow.getCell(c);
                        seedRow.add(cell != null ? cell.toString() : "");
                    }
                }
            }
        } else {
            throw new FileNotFoundException("Input file bd.xlsx not found at " + inputPath);
        }

        System.out.println("Excel Headers read: " + headers);
        System.out.println("Excel Seed Row read: " + seedRow);

        // 生成配置
        int targetSizeMb = 100;      // 目标文件大小（单位为 MB）
        
        // 动态估算生成指定大小 CSV 所需的总行数
        long totalRows = estimateTotalRowsForCsv(headers, seedRow, targetSizeMb, fieldGroupSizes);

        long startTime = System.currentTimeMillis();
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outputPath), StandardCharsets.UTF_8))) {
            
            // 写入 UTF-8 BOM
            writer.write('\ufeff');

            // 动态写入全部表头
            for (int i = 0; i < headers.size(); i++) {
                writer.write(escapeCsv(headers.get(i)));
                if (i < headers.size() - 1) {
                    writer.write(",");
                }
            }
            writer.write("\n");

            Map<Long, String> countryCache = new HashMap<>();

            for (long globalRowIndex = 0; globalRowIndex < totalRows; globalRowIndex++) {
                for (int i = 0; i < headers.size(); i++) {
                    String colName = headers.get(i).toLowerCase();
                    String seedVal = i < seedRow.size() ? seedRow.get(i) : "";
                    String finalVal = generateValue(colName, seedVal, globalRowIndex, fieldGroupSizes, countryCache);

                    writer.write(escapeCsv(finalVal));
                    if (i < headers.size() - 1) {
                        writer.write(",");
                    }
                }
                writer.write("\n");
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Successfully generated " + totalRows + " rows to " + outputPath);
        System.out.println("Time taken: " + (endTime - startTime) + " ms");
    }

    @Test
    public void generateExcelData() throws Exception {
        String inputPath = "d:/fengyong/RuoYi-Cloud-3.6.4/bd.xlsx";
        String outputPath = "d:/fengyong/RuoYi-Cloud-3.6.4/bd_million_java_100m.xlsx";

        // 配置：各个字段的分组大小（单位：行）。未配置的字段将直接使用种子数据不变。
        // - 如果设置为 1：表示该字段逐行自增。
        // - 如果设置为 N (N > 1)：表示该字段每 N 行换一个值（分组）。
        // - 未配置：该字段始终使用 Excel 的种子行原始值。
        Map<String, Integer> fieldGroupSizes = new HashMap<>();
        fieldGroupSizes.put("bdnm", 500);
        fieldGroupSizes.put("bdfh", 500);
        fieldGroupSizes.put("存量1", 500);
        fieldGroupSizes.put("country", 2000);

        List<String> headers = new ArrayList<>();
        List<String> seedRow = new ArrayList<>();

        File file = new File(inputPath);
        if (file.exists()) {
            try (InputStream is = new FileInputStream(file);
                 Workbook workbook = WorkbookFactory.create(is)) {
                Sheet sheet = workbook.getSheetAt(0);
                
                // 读取全部表头 (第0行)
                Row headerRow = sheet.getRow(0);
                if (headerRow != null) {
                    for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                        Cell cell = headerRow.getCell(c);
                        headers.add(cell != null ? cell.toString().trim() : "");
                    }
                }
                
                // 读取第一行作为种子数据 (第1行)
                Row dataRow = sheet.getRow(1);
                if (dataRow != null) {
                    for (int c = 0; c < headers.size(); c++) {
                        Cell cell = dataRow.getCell(c);
                        seedRow.add(cell != null ? cell.toString() : "");
                    }
                }
            }
        } else {
            throw new FileNotFoundException("Input file bd.xlsx not found at " + inputPath);
        }

        System.out.println("Excel Headers read: " + headers);
        System.out.println("Excel Seed Row read: " + seedRow);

        // 生成配置
        int targetSizeMb = 100;      // 目标文件大小（单位为 MB）
        int maxRowsPerSheet = 1000000; // 单个Sheet的最大数据行数限制

        // 动态估算生成指定大小 Excel 所需的总行数
        long totalRows = estimateTotalRows(headers, seedRow, targetSizeMb, fieldGroupSizes);

        long startTime = System.currentTimeMillis();

        // 使用流式工作簿 SXSSFWorkbook 以极低的内存占用生成海量 Excel
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             OutputStream os = new FileOutputStream(outputPath)) {

            int sheetCount = 0;
            Sheet currentSheet = null;
            int rowInSheet = 0;

            Map<Long, String> countryCache = new HashMap<>();

            for (long globalRowIndex = 0; globalRowIndex < totalRows; globalRowIndex++) {
                if (currentSheet == null || rowInSheet > maxRowsPerSheet) {
                    sheetCount++;
                    currentSheet = workbook.createSheet("Sheet" + sheetCount);
                    // 创建动态表头
                    Row header = currentSheet.createRow(0);
                    for (int i = 0; i < headers.size(); i++) {
                        header.createCell(i).setCellValue(headers.get(i));
                    }
                    rowInSheet = 1;
                }

                Row row = currentSheet.createRow(rowInSheet);
                for (int i = 0; i < headers.size(); i++) {
                    String colName = headers.get(i).toLowerCase();
                    String seedVal = i < seedRow.size() ? seedRow.get(i) : "";
                    String finalVal = generateValue(colName, seedVal, globalRowIndex, fieldGroupSizes, countryCache);
                    row.createCell(i).setCellValue(finalVal);
                }

                rowInSheet++;
            }

            workbook.write(os);
            workbook.dispose();
            
            System.out.println("Successfully generated Excel with " + sheetCount + " sheets to " + outputPath);
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Time taken for Excel: " + (endTime - startTime) + " ms");
    }

    private long estimateTotalRows(List<String> headers, List<String> seedRow, int targetSizeMb, 
                                   Map<String, Integer> fieldGroupSizes) throws Exception {
        int pilotRows = 5000;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("Pilot");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
            }

            Map<Long, String> countryCache = new HashMap<>();

            for (int r = 1; r <= pilotRows; r++) {
                long globalRowIndex = r - 1;
                Row row = sheet.createRow(r);
                for (int i = 0; i < headers.size(); i++) {
                    String colName = headers.get(i).toLowerCase();
                    String seedVal = i < seedRow.size() ? seedRow.get(i) : "";
                    String finalVal = generateValue(colName, seedVal, globalRowIndex, fieldGroupSizes, countryCache);
                    row.createCell(i).setCellValue(finalVal);
                }
            }
            workbook.write(bos);
        }

        long compressedPilotSize = bos.size();
        double bytesPerRow = (double) compressedPilotSize / pilotRows;
        long targetBytes = (long) targetSizeMb * 1024 * 1024;
        long totalRows = (long) Math.max(1, Math.round(targetBytes / bytesPerRow));
        System.out.println("Excel Pilot Run: Sample size 5000 rows, compressed size in memory = " + (compressedPilotSize / 1024.0) + " KB");
        System.out.println("Excel Pilot Run: Estimated bytes per row = " + bytesPerRow + ", target rows = " + totalRows);
        return totalRows;
    }

    private long estimateTotalRowsForCsv(List<String> headers, List<String> seedRow, int targetSizeMb, 
                                         Map<String, Integer> fieldGroupSizes) throws Exception {
        int pilotRows = 5000;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bos, StandardCharsets.UTF_8))) {
            
            for (int i = 0; i < headers.size(); i++) {
                writer.write(escapeCsv(headers.get(i)));
                if (i < headers.size() - 1) {
                    writer.write(",");
                }
            }
            writer.write("\n");

            Map<Long, String> countryCache = new HashMap<>();

            for (int r = 1; r <= pilotRows; r++) {
                long globalRowIndex = r - 1;
                for (int i = 0; i < headers.size(); i++) {
                    String colName = headers.get(i).toLowerCase();
                    String seedVal = i < seedRow.size() ? seedRow.get(i) : "";
                    String finalVal = generateValue(colName, seedVal, globalRowIndex, fieldGroupSizes, countryCache);
                    writer.write(escapeCsv(finalVal));
                    if (i < headers.size() - 1) {
                        writer.write(",");
                    }
                }
                writer.write("\n");
            }
        }
        long size = bos.size();
        double bytesPerRow = (double) size / pilotRows;
        long targetBytes = (long) targetSizeMb * 1024 * 1024;
        long totalRows = (long) Math.max(1, Math.round(targetBytes / bytesPerRow));
        System.out.println("CSV Pilot Run: Sample size 5000 rows, size in memory = " + (size / 1024.0) + " KB");
        System.out.println("CSV Pilot Run: Estimated bytes per row = " + bytesPerRow + ", target rows = " + totalRows);
        return totalRows;
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
