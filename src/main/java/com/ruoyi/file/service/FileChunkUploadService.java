package com.ruoyi.file.service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 分片压缩处理，结合线程并发技术，将大文件拆分为多个切片并发传输
 *
 * 注：在真实的 RuoYi-Cloud 项目中，此类应加上 @Service 纳入 Spring 容器管理，
 * 并使用 @Slf4j 进行日志记录。
 */
// @Service
// @Slf4j
public class FileChunkUploadService {

    private static final Logger log = Logger.getLogger(FileChunkUploadService.class.getName());

    // 默认分片大小: 5MB
    private static final int DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024;
    // 线程池大小
    private static final int THREAD_POOL_SIZE = 5;

    // 实例级线程池，不应在方法中关闭，以便复用
    private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    /**
     * 上传大文件：分片、压缩、并发传输
     *
     * @param sourceFile 待上传的文件
     * @param chunkSize  分片大小（字节）
     */
    public void uploadLargeFile(File sourceFile, int chunkSize) {
        if (sourceFile == null || !sourceFile.exists()) {
            log.severe("文件不存在");
            return;
        }

        long fileLength = sourceFile.length();
        int chunks = (int) Math.ceil((double) fileLength / chunkSize);
        log.info("开始上传文件: " + sourceFile.getName() + "，总大小: " + fileLength + "，分为 " + chunks + " 个切片");

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < chunks; i++) {
            final int chunkIndex = i;
            long offset = (long) i * chunkSize;
            long length = Math.min(chunkSize, fileLength - offset);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // 1. 读取当前分片数据
                    byte[] chunkData = readChunk(sourceFile, offset, (int) length);

                    // 2. 压缩当前分片数据
                    byte[] compressedData = compressChunk(chunkData, chunkIndex);

                    // 3. 并发传输
                    transmitChunk(sourceFile.getName(), chunkIndex, compressedData);

                } catch (Exception e) {
                    log.log(Level.SEVERE, "处理分片 " + chunkIndex + " 时出错: " + e.getMessage(), e);
                }
            }, executorService);

            futures.add(future);
        }

        // 等待所有分片上传完成
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            allOf.join();
            log.info("所有切片传输完成，通知服务端合并...");
            notifyServerToMerge(sourceFile.getName(), chunks);
        } catch (Exception e) {
            log.log(Level.SEVERE, "传输过程中发生错误: " + e.getMessage(), e);
        }
        // 注意：不在这里调用 executorService.shutdown()，保证 Service 的复用性
    }

    public void uploadLargeFile(File sourceFile) {
        uploadLargeFile(sourceFile, DEFAULT_CHUNK_SIZE);
    }

    /**
     * 使用 RandomAccessFile 读取文件的指定部分
     */
    private byte[] readChunk(File file, long offset, int length) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(offset);
            byte[] buffer = new byte[length];
            raf.readFully(buffer);
            return buffer;
        }
    }

    /**
     * 压缩分片数据
     */
    private byte[] compressChunk(byte[] chunkData, int chunkIndex) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("chunk_" + chunkIndex);
            zos.putNextEntry(entry);
            zos.write(chunkData);
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    /**
     * 传输分片到服务端
     *
     * 注：此处为模拟实现。在真实业务中，需替换为实际的网络请求逻辑（如 HttpClient/RestTemplate）
     */
    private void transmitChunk(String fileName, int chunkIndex, byte[] data) {
        log.info("成功传输切片: " + chunkIndex + "，原文件名: " + fileName + "，压缩后大小: " + data.length + " bytes");
        try {
            // 模拟网络延迟
            Thread.sleep((long) (Math.random() * 500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 通知服务端合并切片
     *
     * 注：此处为模拟实现。在真实业务中，需向服务端发送合并指令。
     */
    private void notifyServerToMerge(String fileName, int totalChunks) {
        log.info("通知服务端合并文件: " + fileName + "，总切片数: " + totalChunks);
    }
}
