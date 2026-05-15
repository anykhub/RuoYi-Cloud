package com.ruoyi.common.importexport.example;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import com.alibaba.fastjson2.JSON;

public class ExampleDetailDTOConverter implements Converter<ExampleDetailDTO> {

    @Override
    public Class<?> supportJavaTypeKey() {
        return ExampleDetailDTO.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    @Override
    public ExampleDetailDTO convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
                                              GlobalConfiguration globalConfiguration) {
        return JSON.parseObject(cellData.getStringValue(), ExampleDetailDTO.class);
    }

    @Override
    public WriteCellData<?> convertToExcelData(ExampleDetailDTO value, ExcelContentProperty contentProperty,
                                               GlobalConfiguration globalConfiguration) {
        return new WriteCellData<>(JSON.toJSONString(value));
    }
}
