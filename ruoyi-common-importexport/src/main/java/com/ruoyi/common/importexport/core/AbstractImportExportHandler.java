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
    public void exportBigData(java.util.function.Function<Integer, List<T>> pageDataLoader, Class<T> clazz, OutputStream os) {
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
                nextBatch = null; // consume it
                return result;
            }
        };

        doExportBigData(iterable, clazz, os);
    }

    @Override
    public List<T> importData(InputStream is, Class<T> clazz) {
        if (is == null) {
            throw new IllegalArgumentException("输入流不能为空");
        }
        return doImport(is, clazz);
    }

    @Override
    public void importData(InputStream is, Class<T> clazz, int batchSize, java.util.function.Consumer<List<T>> batchConsumer) {
        if (is == null) {
            throw new IllegalArgumentException("输入流不能为空");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("批次大小必须大于0");
        }
        if (batchConsumer == null) {
            throw new IllegalArgumentException("消费函数不能为空");
        }
        doImport(is, clazz, batchSize, batchConsumer);
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

    /**
     * 实际的分批导入操作。
     * 默认实现会退化为一次性读取全部数据后再进行分批回调，子类应当尽可能覆盖此方法实现真正的流式读取。
     *
     * @param is 输入流
     * @param clazz 目标类
     * @param batchSize 批次大小
     * @param batchConsumer 批次数据消费函数
     */
    protected void doImport(InputStream is, Class<T> clazz, int batchSize, java.util.function.Consumer<List<T>> batchConsumer) {
        List<T> allData = doImport(is, clazz);
        if (allData == null || allData.isEmpty()) {
            return;
        }
        int total = allData.size();
        for (int i = 0; i < total; i += batchSize) {
            int end = Math.min(i + batchSize, total);
            batchConsumer.accept(allData.subList(i, end));
        }
    }
}
