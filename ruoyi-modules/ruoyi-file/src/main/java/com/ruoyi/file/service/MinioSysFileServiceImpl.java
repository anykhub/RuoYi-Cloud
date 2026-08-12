package com.ruoyi.file.service;

import java.io.InputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.nacos.common.utils.IoUtils;
import com.ruoyi.file.config.MinioConfig;
import com.ruoyi.file.utils.FileUploadUtils;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;

/**
 * Minio 文件存储
 *
 * @author ruoyi
 */
@Service
public class MinioSysFileServiceImpl implements ISysFileService
{
    @Autowired
    private MinioConfig minioConfig;

    @Autowired
    private MinioClient client;

    /**
     * Minio文件上传接口
     *
     * @param file 上传的文件
     * @return 访问地址
     * @throws Exception
     */
    @Override
    public String uploadFile(MultipartFile file) throws Exception
    {
        String fileName = FileUploadUtils.extractFilename(file);
        InputStream inputStream = file.getInputStream();
        PutObjectArgs args = PutObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(fileName)
                .stream(inputStream, file.getSize(), -1)
                .contentType(file.getContentType())
                .build();
        client.putObject(args);
        IoUtils.closeQuietly(inputStream);
        return minioConfig.getUrl() + "/" + minioConfig.getBucketName() + "/" + fileName;
    }

    /**
     * Minio并发分片压缩上传接口
     *
     * @param file 上传的文件
     * @return 访问地址
     * @throws Exception
     */
    @Override
    public String uploadFileConcurrent(MultipartFile file) throws Exception
    {
        // 先利用工具类将文件在本地进行并发分片压缩并合并成.gz文件
        // 实际上可以做成上传流的形式，但由于MinIO原生支持内部并发上传流（底层已处理）
        // 这里为了完全契合"对上传文件进行分片压缩处理，结合线程并发技术"的需求，
        // 我们可以先使用工具类生成压缩后的临时合并文件，然后通过流上传到MinIO，再删除临时文件。
        String tempDir = System.getProperty("java.io.tmpdir");
        String tempFilePath = FileUploadUtils.uploadConcurrent(tempDir, file);
        java.io.File tempFile = new java.io.File(tempDir, tempFilePath);

        try
        {
            String fileName = tempFile.getName();
            try (InputStream inputStream = new java.io.FileInputStream(tempFile))
            {
                PutObjectArgs args = PutObjectArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .object(fileName)
                        .stream(inputStream, tempFile.length(), -1)
                        .contentType("application/gzip")
                        .build();
                client.putObject(args);
            }
            return minioConfig.getUrl() + "/" + minioConfig.getBucketName() + "/" + fileName;
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
