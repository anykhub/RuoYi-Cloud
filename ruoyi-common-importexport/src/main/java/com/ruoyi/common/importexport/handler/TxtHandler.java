package com.ruoyi.common.importexport.handler;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.importexport.core.AbstractImportExportHandler;
import com.ruoyi.common.importexport.enums.FileTypeEnum;
import com.ruoyi.common.importexport.exception.ImportExportException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * TXT 导入导出处理器 (基于 Fastjson2 将单行数据序列化/反序列化)
 *
 * @author ruoyi
 */
@Slf4j
@Component
public class TxtHandler<T> extends AbstractImportExportHandler<T> {

    @Override
    public FileTypeEnum getSupportedFileType() {
        return FileTypeEnum.TXT;
    }

    @Override
    protected void doExport(List<T> data, Class<T> clazz, OutputStream os) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            for (T item : data) {
                // 每行保存为一个 JSON 对象字符串
                writer.write(JSON.toJSONString(item));
                writer.newLine();
            }
            writer.flush();
        } catch (Exception e) {
            log.error("TXT导出异常", e);
            throw new ImportExportException("TXT导出异常: " + e.getMessage(), e);
        }
    }

    @Override
    protected List<T> doImport(InputStream is, Class<T> clazz) {
        List<T> resultList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    T item = JSON.parseObject(line, clazz);
                    if (item != null) {
                        resultList.add(item);
                    }
                } catch (Exception e) {
                    log.warn("TXT行解析异常: {}", line, e);
                }
            }
        } catch (Exception e) {
            log.error("TXT导入异常", e);
            throw new ImportExportException("TXT导入异常: " + e.getMessage(), e);
        }
        return resultList;
    }
}
