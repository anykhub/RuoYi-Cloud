package com.ruoyi.patcher;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import javassist.*;

public class JavassistPatcher {
    public static void main(String[] args) {
        // Use relative paths to make the project portable
        String jarPathStr = "D:\\fengyong\\RuoYi-Cloud-3.6.4\\ruoyi-modules\\ruoyi-file\\target\\ruoyi-modules-file.jar";
        String tempDirStr = "target/temp_extract";

        File jarFile = new File(jarPathStr);
        File tempDir = new File(tempDirStr);
        File backupJar = new File(jarPathStr + ".bak");

        System.out.println("Starting bytecode patching process...");
        System.out.println("Target JAR: " + jarFile.getAbsolutePath());
        System.out.println("Temp Extract Dir: " + tempDir.getAbsolutePath());

        // 0. If backup exists, restore it first to start fresh
        if (backupJar.exists()) {
            System.out.println("Restoring original JAR from backup to start fresh...");
            try {
                Files.copy(backupJar.toPath(), jarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Original JAR restored successfully.");
            } catch (IOException e) {
                System.err.println("Warning: failed to restore from backup jar: " + e.getMessage());
            }
        }

        try {
            // 1. Clean and create temp directory
            if (tempDir.exists()) {
                deleteDirectory(tempDir);
            }
            tempDir.mkdirs();

            // 2. Unzip the target JAR
            System.out.println("Extracting JAR...");
            unzip(jarFile, tempDir);

            // 3. Configure Javassist ClassPool
            System.out.println("Setting up Javassist ClassPool...");
            ClassPool pool = ClassPool.getDefault();
            
            // Append system classpath
            pool.appendSystemPath();
            
            // Append application classes from BOOT-INF/classes
            String classesPath = new File(tempDir, "BOOT-INF/classes").getAbsolutePath();
            pool.insertClassPath(classesPath);
            System.out.println("Added to ClassPool: " + classesPath);

            // Append dependencies from BOOT-INF/lib
            File libDir = new File(tempDir, "BOOT-INF/lib");
            if (libDir.exists() && libDir.isDirectory()) {
                File[] jars = libDir.listFiles();
                if (jars != null) {
                    for (File jar : jars) {
                        if (jar.getName().endsWith(".jar")) {
                            pool.insertClassPath(jar.getAbsolutePath());
                        }
                    }
                    System.out.println("Added " + jars.length + " dependency JARs to ClassPool.");
                }
            }

            // 4. Modify com.ruoyi.file.controller.SysFileController
            String targetClassName = "com.ruoyi.file.controller.SysFileController";
            System.out.println("Loading class: " + targetClassName);
            CtClass cc = pool.get(targetClassName);

            System.out.println("Finding method: upload");
            CtMethod method = cc.getDeclaredMethod("upload", new CtClass[]{
                pool.get("org.springframework.web.multipart.MultipartFile")
            });

            System.out.println("Modifying method upload body...");
            
            // Replace the method body completely
            method.setBody(
                "{" +
                "    try {" +
                "        System.out.println(\"[Antigravity Patched Controller] === upload Method Started ===\");" +
                "        System.out.println(\"[Antigravity Patched Controller] File name: \" + $1.getOriginalFilename());" +
                "        String url = sysFileService.uploadFile($1);" +
                "        com.ruoyi.system.api.domain.SysFile sysFile = new com.ruoyi.system.api.domain.SysFile();" +
                "        sysFile.setName(com.ruoyi.common.core.utils.file.FileUtils.getName(url));" +
                "        sysFile.setUrl(url + \"?patched=true\");" + // Modified url to append '?patched=true'
                "        System.out.println(\"[Antigravity Patched Controller] File uploaded successfully, URL: \" + url);" +
                "        return com.ruoyi.common.core.domain.R.ok(sysFile);" +
                "    } catch (Exception e) {" +
                "        log.error(\"上传文件失败\", e);" +
                "        return com.ruoyi.common.core.domain.R.fail(e.getMessage());" +
                "    }" +
                "}"
            );

            System.out.println("Writing modified class back to disk...");
            cc.writeFile(classesPath);
            cc.detach();
            System.out.println("Class modified and saved successfully.");

            // 5. Repack ZIP back to JAR (only create backup if it doesn't already exist)
            if (!backupJar.exists()) {
                Files.copy(jarFile.toPath(), backupJar.toPath());
                System.out.println("Created backup JAR at: " + backupJar.getAbsolutePath());
            }

            System.out.println("Repackaging JAR...");
            File tempOutputJar = new File(jarPathStr + ".tmp");
            if (tempOutputJar.exists()) {
                tempOutputJar.delete();
            }
            zipDirectory(tempDir, tempOutputJar);

            // Replace original JAR with modified one
            if (jarFile.delete()) {
                if (tempOutputJar.renameTo(jarFile)) {
                    System.out.println("Successfully replaced original JAR with the patched JAR!");
                } else {
                    throw new IOException("Failed to rename temporary patched jar to target jar!");
                }
            } else {
                throw new IOException("Failed to delete original target jar to replace it!");
            }

            // 6. Clean up
            System.out.println("Cleaning up temporary directory...");
            deleteDirectory(tempDir);
            System.out.println("Bytecode patching completed successfully!");

        } catch (Exception e) {
            System.err.println("Error occurred during bytecode patching: ");
            e.printStackTrace();
        }
    }

    private static void unzip(File zipFile, File destDir) throws IOException {
        byte[] buffer = new byte[8192];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(destDir, zipEntry);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
        if (!destFilePath.startsWith(destDirPath + File.separator) && !destFilePath.equals(destDirPath)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        return destFile;
    }

    private static void zipDirectory(File sourceDir, File outputZip) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputZip))) {
            Path sourcePath = sourceDir.toPath();
            Files.walk(sourcePath)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    String zipEntryName = sourcePath.relativize(path).toString().replace('\\', '/');
                    try {
                        ZipEntry zipEntry = new ZipEntry(zipEntryName);
                        zos.putNextEntry(zipEntry);
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        }
    }

    private static void deleteDirectory(File dir) {
        File[] allContents = dir.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        dir.delete();
    }
}
