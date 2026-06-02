package com.ruoyi.file.service;

import java.io.InputStream;
import com.alibaba.nacos.common.utils.IoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.ruoyi.common.core.utils.file.FileTypeUtils;

/**
 * FastDFS 文件存储
 *
 * @author ruoyi
 */
@Service
public class FastDfsSysFileServiceImpl implements ISysFileService
{
    /**
     * 域名或本机访问地址
     */
    @Value("${fdfs.domain}")
    public String domain;

    @Autowired
    private FastFileStorageClient storageClient;

    /**
     * FastDfs文件上传接口
     *
     * @param file 上传的文件
     * @return 访问地址
     * @throws Exception
     */
    @Override
    public String uploadFile(MultipartFile file) throws Exception
    {
        InputStream inputStream = file.getInputStream();
        StorePath storePath = storageClient.uploadFile(inputStream, file.getSize(),
                FileTypeUtils.getExtension(file), null);
        IoUtils.closeQuietly(inputStream);
        return domain + "/" + storePath.getFullPath();
    }

    /**
     * FastDfs并发分片压缩上传接口
     *
     * @param file 上传的文件
     * @return 访问地址
     * @throws Exception
     */
    @Override
    public String uploadFileConcurrent(MultipartFile file) throws Exception
    {
        // 借助通用工具类实现并发分片压缩
        String tempDir = System.getProperty("java.io.tmpdir");
        String tempFilePath = com.ruoyi.file.utils.FileUploadUtils.uploadConcurrent(tempDir, file);
        java.io.File tempFile = new java.io.File(tempDir, tempFilePath);

        try
        {
            try (InputStream inputStream = new java.io.FileInputStream(tempFile))
            {
                StorePath storePath = storageClient.uploadFile(inputStream, tempFile.length(),
                        "gz", null);
                return domain + "/" + storePath.getFullPath();
            }
        }
        finally
        {
            if (tempFile.exists())
            {
                tempFile.delete();
            }
        }
    }
}
