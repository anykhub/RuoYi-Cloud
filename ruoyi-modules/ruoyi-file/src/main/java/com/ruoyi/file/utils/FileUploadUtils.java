package com.ruoyi.file.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Objects;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.exception.file.FileException;
import com.ruoyi.common.core.exception.file.FileNameLengthLimitExceededException;
import com.ruoyi.common.core.exception.file.FileSizeLimitExceededException;
import com.ruoyi.common.core.exception.file.InvalidExtensionException;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.utils.file.FileTypeUtils;
import com.ruoyi.common.core.utils.file.MimeTypeUtils;
import com.ruoyi.common.core.utils.uuid.Seq;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPOutputStream;

/**
 * 文件上传工具类
 * 
 * @author ruoyi
 */
public class FileUploadUtils
{
    /**
     * 默认大小 50M
     */
    public static final long DEFAULT_MAX_SIZE = 50 * 1024 * 1024;

    /**
     * 默认的文件名最大长度 100
     */
    public static final int DEFAULT_FILE_NAME_LENGTH = 100;

    /**
     * 根据文件路径上传
     *
     * @param baseDir 相对应用的基目录
     * @param file 上传的文件
     * @return 文件名称
     * @throws IOException
     */
    public static final String upload(String baseDir, MultipartFile file) throws IOException
    {
        try
        {
            return upload(baseDir, file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION);
        }
        catch (FileException fe)
        {
            throw new IOException(fe.getDefaultMessage(), fe);
        }
        catch (Exception e)
        {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * 文件上传
     *
     * @param baseDir 相对应用的基目录
     * @param file 上传的文件
     * @param allowedExtension 上传文件类型
     * @return 返回上传成功的文件名
     * @throws FileSizeLimitExceededException 如果超出最大大小
     * @throws FileNameLengthLimitExceededException 文件名太长
     * @throws IOException 比如读写文件出错时
     * @throws InvalidExtensionException 文件校验异常
     */
    public static final String upload(String baseDir, MultipartFile file, String[] allowedExtension)
            throws FileSizeLimitExceededException, IOException, FileNameLengthLimitExceededException,
            InvalidExtensionException
    {
        int fileNamelength = Objects.requireNonNull(file.getOriginalFilename()).length();
        if (fileNamelength > FileUploadUtils.DEFAULT_FILE_NAME_LENGTH)
        {
            throw new FileNameLengthLimitExceededException(FileUploadUtils.DEFAULT_FILE_NAME_LENGTH);
        }

        assertAllowed(file, allowedExtension);

        String fileName = extractFilename(file);

        String absPath = getAbsoluteFile(baseDir, fileName).getAbsolutePath();
        file.transferTo(Paths.get(absPath));
        return getPathFileName(fileName);
    }

    /**
     * 根据文件路径并发分片压缩上传
     *
     * @param baseDir 相对应用的基目录
     * @param file 上传的文件
     * @return 文件名称
     * @throws IOException
     */
    public static final String uploadConcurrent(String baseDir, MultipartFile file) throws IOException
    {
        try
        {
            return uploadConcurrent(baseDir, file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION);
        }
        catch (FileException fe)
        {
            throw new IOException(fe.getDefaultMessage(), fe);
        }
        catch (Exception e)
        {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * 并发分片压缩上传文件
     *
     * @param baseDir 相对应用的基目录
     * @param file 上传的文件
     * @param allowedExtension 上传文件类型
     * @return 返回上传成功的文件名
     * @throws Exception
     */
    public static final String uploadConcurrent(String baseDir, MultipartFile file, String[] allowedExtension)
            throws Exception
    {
        int fileNamelength = Objects.requireNonNull(file.getOriginalFilename()).length();
        if (fileNamelength > FileUploadUtils.DEFAULT_FILE_NAME_LENGTH)
        {
            throw new FileNameLengthLimitExceededException(FileUploadUtils.DEFAULT_FILE_NAME_LENGTH);
        }

        assertAllowed(file, allowedExtension);

        String fileName = extractFilename(file);
        fileName = fileName + ".gz"; // 追加压缩后缀

        File absFile = getAbsoluteFile(baseDir, fileName);
        String absPath = absFile.getAbsolutePath();

        long chunkSize = 5 * 1024 * 1024; // 5MB per chunk
        long fileSize = file.getSize();
        int chunkCount = (int) Math.ceil((double) fileSize / chunkSize);

        if (chunkCount <= 1)
        {
            try (java.io.InputStream in = file.getInputStream();
                 FileOutputStream fos = new FileOutputStream(absPath);
                 GZIPOutputStream gos = new GZIPOutputStream(fos))
            {
                org.apache.commons.io.IOUtils.copy(in, gos);
            }
            return getPathFileName(fileName);
        }

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(chunkCount, 10));
        List<Future<File>> futures = new ArrayList<>();
        List<File> chunkFilesToClean = new ArrayList<>();

        File tempFile = File.createTempFile("upload_", ".tmp");
        file.transferTo(tempFile);

        try
        {
            for (int i = 0; i < chunkCount; i++)
            {
                final int chunkIndex = i;
                final long start = i * chunkSize;
                final long length = Math.min(chunkSize, fileSize - start);

                File chunkFile = File.createTempFile("chunk_" + chunkIndex + "_", ".gz");
                chunkFilesToClean.add(chunkFile);

                futures.add(executor.submit(() -> {
                    try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(tempFile, "r");
                         FileOutputStream fos = new FileOutputStream(chunkFile);
                         GZIPOutputStream gos = new GZIPOutputStream(fos))
                    {
                        raf.seek(start);
                        byte[] buffer = new byte[8192];
                        long bytesReadTotal = 0;
                        int bytesRead;
                        while (bytesReadTotal < length && (bytesRead = raf.read(buffer, 0, (int) Math.min(buffer.length, length - bytesReadTotal))) != -1)
                        {
                            gos.write(buffer, 0, bytesRead);
                            bytesReadTotal += bytesRead;
                        }
                    }
                    return chunkFile;
                }));
            }

            List<File> chunkFiles = new ArrayList<>();
            for (Future<File> future : futures)
            {
                chunkFiles.add(future.get());
            }

            try (FileOutputStream mergedFos = new FileOutputStream(absFile))
            {
                for (File chunk : chunkFiles)
                {
                    try (FileInputStream fis = new FileInputStream(chunk))
                    {
                        org.apache.commons.io.IOUtils.copy(fis, mergedFos);
                    }
                }
            }
        }
        finally
        {
            executor.shutdownNow();
            tempFile.delete();
            for (File chunk : chunkFilesToClean)
            {
                if (chunk.exists())
                {
                    chunk.delete();
                }
            }
        }

        return getPathFileName(fileName);
    }

    /**
     * 编码文件名
     */
    public static final String extractFilename(MultipartFile file)
    {
        return StringUtils.format("{}/{}_{}.{}", DateUtils.datePath(),
                FilenameUtils.getBaseName(file.getOriginalFilename()), Seq.getId(Seq.uploadSeqType), FileTypeUtils.getExtension(file));
    }

    private static final File getAbsoluteFile(String uploadDir, String fileName) throws IOException
    {
        File desc = new File(uploadDir + File.separator + fileName);

        if (!desc.exists())
        {
            if (!desc.getParentFile().exists())
            {
                desc.getParentFile().mkdirs();
            }
        }
        return desc.isAbsolute() ? desc : desc.getAbsoluteFile();
    }

    private static final String getPathFileName(String fileName) throws IOException
    {
        String pathFileName = "/" + fileName;
        return pathFileName;
    }

    /**
     * 文件大小校验
     *
     * @param file 上传的文件
     * @throws FileSizeLimitExceededException 如果超出最大大小
     * @throws InvalidExtensionException 文件校验异常
     */
    public static final void assertAllowed(MultipartFile file, String[] allowedExtension)
            throws FileSizeLimitExceededException, InvalidExtensionException
    {
        long size = file.getSize();
        if (size > DEFAULT_MAX_SIZE)
        {
            throw new FileSizeLimitExceededException(DEFAULT_MAX_SIZE / 1024 / 1024);
        }

        String fileName = file.getOriginalFilename();
        String extension = FileTypeUtils.getExtension(file);
        if (allowedExtension != null && !isAllowedExtension(extension, allowedExtension))
        {
            if (allowedExtension == MimeTypeUtils.IMAGE_EXTENSION)
            {
                throw new InvalidExtensionException.InvalidImageExtensionException(allowedExtension, extension,
                        fileName);
            }
            else if (allowedExtension == MimeTypeUtils.FLASH_EXTENSION)
            {
                throw new InvalidExtensionException.InvalidFlashExtensionException(allowedExtension, extension,
                        fileName);
            }
            else if (allowedExtension == MimeTypeUtils.MEDIA_EXTENSION)
            {
                throw new InvalidExtensionException.InvalidMediaExtensionException(allowedExtension, extension,
                        fileName);
            }
            else if (allowedExtension == MimeTypeUtils.VIDEO_EXTENSION)
            {
                throw new InvalidExtensionException.InvalidVideoExtensionException(allowedExtension, extension,
                        fileName);
            }
            else
            {
                throw new InvalidExtensionException(allowedExtension, extension, fileName);
            }
        }
    }

    /**
     * 判断MIME类型是否是允许的MIME类型
     *
     * @param extension 上传文件类型
     * @param allowedExtension 允许上传文件类型
     * @return true/false
     */
    public static final boolean isAllowedExtension(String extension, String[] allowedExtension)
    {
        for (String str : allowedExtension)
        {
            if (str.equalsIgnoreCase(extension))
            {
                return true;
            }
        }
        return false;
    }
}