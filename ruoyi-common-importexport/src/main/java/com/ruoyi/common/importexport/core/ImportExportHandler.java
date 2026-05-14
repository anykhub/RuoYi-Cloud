package com.ruoyi.common.importexport.core;

import com.ruoyi.common.importexport.enums.FileTypeEnum;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 通用导入导出处理器接口
 *
 * @author ruoyi
 * @param <T> 数据实体泛型
 */
public interface ImportExportHandler<T> {

    /**
     * 获取当前处理器支持的文件类型
     *
     * @return 文件类型枚举
     */
    FileTypeEnum getSupportedFileType();

    /**
     * 导出数据
     *
     * @param data 要导出的数据列表
     * @param os 输出流
     */
    void exportData(List<T> data, OutputStream os);

    /**
     * 导出数据 (带自定义类)
     *
     * @param data 要导出的数据列表
     * @param clazz 目标类
     * @param os 输出流
     */
    void exportData(List<T> data, Class<T> clazz, OutputStream os);

    /**
     * 导入数据
     *
     * @param is 输入流
     * @param clazz 目标类
     * @return 解析并校验后的数据列表（包含校验通过的数据）
     */
    List<T> importData(InputStream is, Class<T> clazz);

}
