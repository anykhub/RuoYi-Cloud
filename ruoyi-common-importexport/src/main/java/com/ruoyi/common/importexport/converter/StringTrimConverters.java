package com.ruoyi.common.importexport.converter;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;

import com.alibaba.excel.util.DateUtils;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 自动去除单元格字符串空格的数字与日期转换器集合
 * 解决 Excel 导入时非字符串单元格（Long, Integer, Double, Float, BigDecimal, Date 等）包含空格或空字符串导致 ExcelDataConvertException 的问题
 *
 * @author ruoyi
 */
public class StringTrimConverters {

    /**
     * Long 类型安全去空格转换器
     */
    public static class LongStringTrimConverter implements Converter<Long> {
        @Override
        public Class<?> supportJavaTypeKey() {
            return Long.class;
        }

        @Override
        public CellDataTypeEnum supportExcelTypeKey() {
            return CellDataTypeEnum.STRING;
        }

        @Override
        public Long convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
                                      GlobalConfiguration globalConfiguration) {
            if (cellData == null) {
                return null;
            }
            if (cellData.getType() == CellDataTypeEnum.NUMBER) {
                return cellData.getNumberValue().longValue();
            }
            String stringValue = cellData.getStringValue();
            if (stringValue == null || stringValue.trim().isEmpty()) {
                return null;
            }
            return Long.valueOf(stringValue.trim());
        }

        @Override
        public WriteCellData<?> convertToExcelData(Long value, ExcelContentProperty contentProperty,
                                                   GlobalConfiguration globalConfiguration) {
            if (value == null) {
                return new WriteCellData<>("");
            }
            return new WriteCellData<>(value.toString());
        }
    }

    /**
     * Integer 类型安全去空格转换器
     */
    public static class IntegerStringTrimConverter implements Converter<Integer> {
        @Override
        public Class<?> supportJavaTypeKey() {
            return Integer.class;
        }

        @Override
        public CellDataTypeEnum supportExcelTypeKey() {
            return CellDataTypeEnum.STRING;
        }

        @Override
        public Integer convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
                                         GlobalConfiguration globalConfiguration) {
            if (cellData == null) {
                return null;
            }
            if (cellData.getType() == CellDataTypeEnum.NUMBER) {
                return cellData.getNumberValue().intValue();
            }
            String stringValue = cellData.getStringValue();
            if (stringValue == null || stringValue.trim().isEmpty()) {
                return null;
            }
            return Integer.valueOf(stringValue.trim());
        }

        @Override
        public WriteCellData<?> convertToExcelData(Integer value, ExcelContentProperty contentProperty,
                                                   GlobalConfiguration globalConfiguration) {
            if (value == null) {
                return new WriteCellData<>("");
            }
            return new WriteCellData<>(value.toString());
        }
    }

    /**
     * Double 类型安全去空格转换器
     */
    public static class DoubleStringTrimConverter implements Converter<Double> {
        @Override
        public Class<?> supportJavaTypeKey() {
            return Double.class;
        }

        @Override
        public CellDataTypeEnum supportExcelTypeKey() {
            return CellDataTypeEnum.STRING;
        }

        @Override
        public Double convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
                                        GlobalConfiguration globalConfiguration) {
            if (cellData == null) {
                return null;
            }
            if (cellData.getType() == CellDataTypeEnum.NUMBER) {
                return cellData.getNumberValue().doubleValue();
            }
            String stringValue = cellData.getStringValue();
            if (stringValue == null || stringValue.trim().isEmpty()) {
                return null;
            }
            return Double.valueOf(stringValue.trim());
        }

        @Override
        public WriteCellData<?> convertToExcelData(Double value, ExcelContentProperty contentProperty,
                                                   GlobalConfiguration globalConfiguration) {
            if (value == null) {
                return new WriteCellData<>("");
            }
            return new WriteCellData<>(value.toString());
        }
    }

    /**
     * Float 类型安全去空格转换器
     */
    public static class FloatStringTrimConverter implements Converter<Float> {
        @Override
        public Class<?> supportJavaTypeKey() {
            return Float.class;
        }

        @Override
        public CellDataTypeEnum supportExcelTypeKey() {
            return CellDataTypeEnum.STRING;
        }

        @Override
        public Float convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
                                       GlobalConfiguration globalConfiguration) {
            if (cellData == null) {
                return null;
            }
            if (cellData.getType() == CellDataTypeEnum.NUMBER) {
                return cellData.getNumberValue().floatValue();
            }
            String stringValue = cellData.getStringValue();
            if (stringValue == null || stringValue.trim().isEmpty()) {
                return null;
            }
            return Float.valueOf(stringValue.trim());
        }

        @Override
        public WriteCellData<?> convertToExcelData(Float value, ExcelContentProperty contentProperty,
                                                   GlobalConfiguration globalConfiguration) {
            if (value == null) {
                return new WriteCellData<>("");
            }
            return new WriteCellData<>(value.toString());
        }
    }

    /**
     * BigDecimal 类型安全去空格转换器
     */
    public static class BigDecimalStringTrimConverter implements Converter<BigDecimal> {
        @Override
        public Class<?> supportJavaTypeKey() {
            return BigDecimal.class;
        }

        @Override
        public CellDataTypeEnum supportExcelTypeKey() {
            return CellDataTypeEnum.STRING;
        }

        @Override
        public BigDecimal convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
                                            GlobalConfiguration globalConfiguration) {
            if (cellData == null) {
                return null;
            }
            if (cellData.getType() == CellDataTypeEnum.NUMBER) {
                return cellData.getNumberValue();
            }
            String stringValue = cellData.getStringValue();
            if (stringValue == null || stringValue.trim().isEmpty()) {
                return null;
            }
            return new BigDecimal(stringValue.trim());
        }

        @Override
        public WriteCellData<?> convertToExcelData(BigDecimal value, ExcelContentProperty contentProperty,
                                                   GlobalConfiguration globalConfiguration) {
            if (value == null) {
                return new WriteCellData<>("");
            }
            return new WriteCellData<>(value.toPlainString());
        }
    }

    /**
     * Short 类型安全去空格转换器
     */
    public static class ShortStringTrimConverter implements Converter<Short> {
        @Override
        public Class<?> supportJavaTypeKey() {
            return Short.class;
        }

        @Override
        public CellDataTypeEnum supportExcelTypeKey() {
            return CellDataTypeEnum.STRING;
        }

        @Override
        public Short convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
                                       GlobalConfiguration globalConfiguration) {
            if (cellData == null) {
                return null;
            }
            if (cellData.getType() == CellDataTypeEnum.NUMBER) {
                return cellData.getNumberValue().shortValue();
            }
            String stringValue = cellData.getStringValue();
            if (stringValue == null || stringValue.trim().isEmpty()) {
                return null;
            }
            return Short.valueOf(stringValue.trim());
        }

        @Override
        public WriteCellData<?> convertToExcelData(Short value, ExcelContentProperty contentProperty,
                                                   GlobalConfiguration globalConfiguration) {
            if (value == null) {
                return new WriteCellData<>("");
            }
            return new WriteCellData<>(value.toString());
        }
    }

    /**
     * Byte 类型安全去空格转换器
     */
    public static class ByteStringTrimConverter implements Converter<Byte> {
        @Override
        public Class<?> supportJavaTypeKey() {
            return Byte.class;
        }

        @Override
        public CellDataTypeEnum supportExcelTypeKey() {
            return CellDataTypeEnum.STRING;
        }

        @Override
        public Byte convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
                                      GlobalConfiguration globalConfiguration) {
            if (cellData == null) {
                return null;
            }
            if (cellData.getType() == CellDataTypeEnum.NUMBER) {
                return cellData.getNumberValue().byteValue();
            }
            String stringValue = cellData.getStringValue();
            if (stringValue == null || stringValue.trim().isEmpty()) {
                return null;
            }
            return Byte.valueOf(stringValue.trim());
        }

        @Override
        public WriteCellData<?> convertToExcelData(Byte value, ExcelContentProperty contentProperty,
                                                   GlobalConfiguration globalConfiguration) {
            if (value == null) {
                return new WriteCellData<>("");
            }
            return new WriteCellData<>(value.toString());
        }
    }

    /**
     * Date 类型安全去空格与空单元格转换器
     */
    public static class DateStringTrimConverter implements Converter<Date> {
        @Override
        public Class<?> supportJavaTypeKey() {
            return Date.class;
        }

        @Override
        public CellDataTypeEnum supportExcelTypeKey() {
            return CellDataTypeEnum.STRING;
        }

        @Override
        public Date convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
                                      GlobalConfiguration globalConfiguration) throws Exception {
            if (cellData == null) {
                return null;
            }
            if (cellData.getType() == CellDataTypeEnum.EMPTY) {
                return null;
            }
            if (cellData.getType() == CellDataTypeEnum.NUMBER) {
                return new com.alibaba.excel.converters.date.DateNumberConverter()
                        .convertToJavaData(cellData, contentProperty, globalConfiguration);
            }
            if (cellData.getType() == CellDataTypeEnum.DATE && cellData.getData() instanceof Date) {
                return (Date) cellData.getData();
            }
            String stringValue = cellData.getStringValue();
            if (stringValue == null || stringValue.trim().isEmpty()) {
                return null;
            }
            stringValue = stringValue.trim();
            if (contentProperty != null && contentProperty.getDateTimeFormatProperty() != null) {
                String format = contentProperty.getDateTimeFormatProperty().getFormat();
                try {
                    return DateUtils.parseDate(stringValue, format);
                } catch (Exception ignored) {
                    // 如果指定格式解析失败，降级尝试自适应格式解析
                }
            }
            return DateUtils.parseDate(stringValue, null);
        }

        @Override
        public WriteCellData<?> convertToExcelData(Date value, ExcelContentProperty contentProperty,
                                                   GlobalConfiguration globalConfiguration) {
            if (value == null) {
                return new WriteCellData<>("");
            }
            String format = (contentProperty != null && contentProperty.getDateTimeFormatProperty() != null)
                    ? contentProperty.getDateTimeFormatProperty().getFormat()
                    : null;
            return new WriteCellData<>(DateUtils.format(value, format));
        }
    }
}
