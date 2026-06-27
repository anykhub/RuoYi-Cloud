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
        "阿根廷", "智利", "哥伦比亚", "秘鲁", "爱尔兰", "比利时", "奥地利", "丹麦", "挪威", "芬兰",
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

    @Test
    public void generateData() throws Exception {
        String inputPath = "d:/fengyong/RuoYi-Cloud-3.6.4/bd.xlsx";
        String outputPath = "d:/fengyong/RuoYi-Cloud-3.6.4/bd_million_java_100m.csv";

        // 配置：哪些字段在生成每一组时自动带上组号后缀（小写匹配）
        Set<String> groupIdentifierFields = new HashSet<>(Arrays.asList("bdnm", "bdfh"));
        
        // 配置：哪些字段需要在组内行级进行序列自增（小写匹配）
        Set<String> autoIncrementFields = new HashSet<>(Arrays.asList("bdnm", "bdfh"));

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
        int rowsPerGroup = 2000;    // 每组生成的行数
        
        // 动态估算生成指定大小 CSV 所需的组数
        int numGroups = estimateTargetGroupsForCsv(headers, seedRow, targetSizeMb, rowsPerGroup, groupIdentifierFields, autoIncrementFields);

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

            for (int g = 0; g < numGroups; g++) {
                String groupSuffix = g > 0 ? String.valueOf(g) : "";
                String grpCountry = REAL_COUNTRIES[ThreadLocalRandom.current().nextInt(REAL_COUNTRIES.length)];

                for (int r = 1; r <= rowsPerGroup; r++) {
                    for (int i = 0; i < headers.size(); i++) {
                        String colName = headers.get(i).toLowerCase();
                        String seedVal = i < seedRow.size() ? seedRow.get(i) : "";
                        String finalVal;

                        if ("country".equals(colName)) {
                            finalVal = grpCountry;
                        } else {
                            finalVal = seedVal;
                            if (autoIncrementFields.contains(colName)) {
                                if (isNumeric(finalVal)) {
                                    long baseNum = parseLongRobust(finalVal);
                                    finalVal = String.valueOf(baseNum + ((long) g * rowsPerGroup) + (r - 1));
                                } else {
                                    finalVal = finalVal + groupSuffix + "_" + r;
                                }
                            } else if (groupIdentifierFields.contains(colName)) {
                                if (isNumeric(finalVal)) {
                                    long baseNum = parseLongRobust(finalVal);
                                    finalVal = String.valueOf(baseNum + g);
                                } else {
                                    finalVal = finalVal + groupSuffix;
                                }
                            }
                        }

                        writer.write(escapeCsv(finalVal));
                        if (i < headers.size() - 1) {
                            writer.write(",");
                        }
                    }
                    writer.write("\n");
                }
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Successfully generated " + (numGroups * rowsPerGroup) + " rows to " + outputPath);
        System.out.println("Time taken: " + (endTime - startTime) + " ms");
    }

    @Test
    public void generateExcelData() throws Exception {
        String inputPath = "d:/fengyong/RuoYi-Cloud-3.6.4/bd.xlsx";
        String outputPath = "d:/fengyong/RuoYi-Cloud-3.6.4/bd_million_java_100m.xlsx";

        // 配置：哪些字段在生成每一组时自动带上组号后缀（小写匹配）
        Set<String> groupIdentifierFields = new HashSet<>(Arrays.asList("bdnm", "bdfh","存量1"));
        
        // 配置：哪些字段需要在组内行级进行序列自增（小写匹配）
        Set<String> autoIncrementFields = new HashSet<>(Arrays.asList("bdnm", "bdfh","存量1"));

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
        int rowsPerGroup = 2000;    // 每组生成的行数
        int maxRowsPerSheet = 1000000; // 单个Sheet的最大数据行数限制

        // 动态估算生成指定大小 Excel 所需的组数
        int numGroups = estimateTargetGroups(headers, seedRow, targetSizeMb, rowsPerGroup, groupIdentifierFields, autoIncrementFields);

        long startTime = System.currentTimeMillis();

        // 使用流式工作簿 SXSSFWorkbook 以极低的内存占用生成海量 Excel
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             OutputStream os = new FileOutputStream(outputPath)) {

            int sheetCount = 0;
            Sheet currentSheet = null;
            int rowInSheet = 0;

            for (int g = 0; g < numGroups; g++) {
                String groupSuffix = g > 0 ? String.valueOf(g) : "";
                String grpCountry = REAL_COUNTRIES[ThreadLocalRandom.current().nextInt(REAL_COUNTRIES.length)];

                for (int r = 1; r <= rowsPerGroup; r++) {
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
                        String finalVal;

                        if ("country".equals(colName)) {
                            finalVal = grpCountry;
                        } else {
                            finalVal = seedVal;
                            if (autoIncrementFields.contains(colName)) {
                                if (isNumeric(finalVal)) {
                                    long baseNum = parseLongRobust(finalVal);
                                    finalVal = String.valueOf(baseNum + ((long) g * rowsPerGroup) + (r - 1));
                                } else {
                                    finalVal = finalVal + groupSuffix + "_" + r;
                                }
                            } else if (groupIdentifierFields.contains(colName)) {
                                if (isNumeric(finalVal)) {
                                    long baseNum = parseLongRobust(finalVal);
                                    finalVal = String.valueOf(baseNum + g);
                                } else {
                                    finalVal = finalVal + groupSuffix;
                                }
                            }
                        }
                        row.createCell(i).setCellValue(finalVal);
                    }

                    rowInSheet++;
                }
            }

            workbook.write(os);
            workbook.dispose();
            
            System.out.println("Successfully generated Excel with " + sheetCount + " sheets to " + outputPath);
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Time taken for Excel: " + (endTime - startTime) + " ms");
    }

    private int estimateTargetGroups(List<String> headers, List<String> seedRow, int targetSizeMb, 
                                     int rowsPerGroup, Set<String> groupIdentifierFields, Set<String> autoIncrementFields) throws Exception {
        int pilotRows = 5000;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("Pilot");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
            }

            for (int r = 1; r <= pilotRows; r++) {
                String grpCountry = REAL_COUNTRIES[ThreadLocalRandom.current().nextInt(REAL_COUNTRIES.length)];
                Row row = sheet.createRow(r);
                for (int i = 0; i < headers.size(); i++) {
                    String colName = headers.get(i).toLowerCase();
                    String seedVal = i < seedRow.size() ? seedRow.get(i) : "";
                    String finalVal;

                    if ("country".equals(colName)) {
                        finalVal = grpCountry;
                    } else {
                        finalVal = seedVal;
                        if (autoIncrementFields.contains(colName)) {
                            if (isNumeric(finalVal)) {
                                long baseNum = parseLongRobust(finalVal);
                                finalVal = String.valueOf(baseNum + (r - 1));
                            } else {
                                finalVal = finalVal + "_" + r;
                            }
                        } else if (groupIdentifierFields.contains(colName)) {
                            finalVal = finalVal; // base estimation keeps unmodified
                        }
                    }
                    row.createCell(i).setCellValue(finalVal);
                }
            }
            workbook.write(bos);
        }

        long compressedPilotSize = bos.size();
        double bytesPerRow = (double) compressedPilotSize / pilotRows;
        long targetBytes = (long) targetSizeMb * 1024 * 1024;
        double totalRowsNeeded = targetBytes / bytesPerRow;
        
        int estimatedGroups = (int) Math.max(1, Math.round(totalRowsNeeded / rowsPerGroup));
        System.out.println("Excel Pilot Run: Sample size 5000 rows, compressed size in memory = " + (compressedPilotSize / 1024.0) + " KB");
        System.out.println("Excel Pilot Run: Estimated bytes per row = " + bytesPerRow + ", target rows = " + Math.round(totalRowsNeeded) + ", estimated groups = " + estimatedGroups);
        return estimatedGroups;
    }

    private int estimateTargetGroupsForCsv(List<String> headers, List<String> seedRow, int targetSizeMb, 
                                           int rowsPerGroup, Set<String> groupIdentifierFields, Set<String> autoIncrementFields) throws Exception {
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

            for (int r = 1; r <= pilotRows; r++) {
                String grpCountry = REAL_COUNTRIES[ThreadLocalRandom.current().nextInt(REAL_COUNTRIES.length)];
                for (int i = 0; i < headers.size(); i++) {
                    String colName = headers.get(i).toLowerCase();
                    String seedVal = i < seedRow.size() ? seedRow.get(i) : "";
                    String finalVal;

                    if ("country".equals(colName)) {
                        finalVal = grpCountry;
                    } else {
                        finalVal = seedVal;
                        if (autoIncrementFields.contains(colName)) {
                            if (isNumeric(finalVal)) {
                                long baseNum = parseLongRobust(finalVal);
                                finalVal = String.valueOf(baseNum + (r - 1));
                            } else {
                                finalVal = finalVal + "_" + r;
                            }
                        } else if (groupIdentifierFields.contains(colName)) {
                            finalVal = finalVal;
                        }
                    }
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
        double totalRowsNeeded = targetBytes / bytesPerRow;
        int estimatedGroups = (int) Math.max(1, Math.round(totalRowsNeeded / rowsPerGroup));
        System.out.println("CSV Pilot Run: Sample size 5000 rows, size in memory = " + (size / 1024.0) + " KB");
        System.out.println("CSV Pilot Run: Estimated bytes per row = " + bytesPerRow + ", target rows = " + Math.round(totalRowsNeeded) + ", estimated groups = " + estimatedGroups);
        return estimatedGroups;
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
