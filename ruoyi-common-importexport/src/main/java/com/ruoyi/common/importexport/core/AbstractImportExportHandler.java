package com.ruoyi.common.importexport.core;

import com.ruoyi.common.importexport.registry.HandlerRegistry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 抽象导入导出处理器
 *
 * 采用模板方法模式，定义导入导出的基本骨架，具体实现交由子类
 *
 * @author ruoyi
 * @param <T> 数据泛型
 */
public abstract class AbstractImportExportHandler<T> implements ImportExportHandler<T>, InitializingBean {

    @Autowired
    private HandlerRegistry handlerRegistry;

    /**
     * 将当前Handler注册到注册中心
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        handlerRegistry.register(this);
    }

    @Override
    public void exportData(List<T> data, OutputStream os) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("导出数据不能为空");
        }
        // 默认使用列表中第一个元素的类作为类型
        Class<?> clazz = data.get(0).getClass();
        doExport(data, (Class<T>) clazz, os);
    }

    @Override
    public void exportData(List<T> data, Class<T> clazz, OutputStream os) {
        doExport(data, clazz, os);
    }

    @Override
    public void exportBigData(Iterable<List<T>> dataIterable, Class<T> clazz, OutputStream os) {
        if (dataIterable == null) {
            throw new IllegalArgumentException("导出数据迭代器不能为空");
        }
        doExportBigData(dataIterable, clazz, os);
    }

    @Override
    public List<T> importData(InputStream is, Class<T> clazz) {
        if (is == null) {
            throw new IllegalArgumentException("输入流不能为空");
        }
        return doImport(is, clazz);
    }

    /**
     * 实际的导出操作，由子类实现
     *
     * @param data 要导出的数据
     * @param clazz 目标类
     * @param os 输出流
     */
    protected abstract void doExport(List<T> data, Class<T> clazz, OutputStream os);

    /**
     * 实际的大数据分批导出操作，由子类实现
     *
     * @param dataIterable 批次数据迭代器
     * @param clazz 目标类
     * @param os 输出流
     */
    protected abstract void doExportBigData(Iterable<List<T>> dataIterable, Class<T> clazz, OutputStream os);

    /**
     * 实际的导入操作，由子类实现
     *
     * @param is 输入流
     * @param clazz 目标类
     * @return 导入的数据列表
     */
    protected abstract List<T> doImport(InputStream is, Class<T> clazz);
}
