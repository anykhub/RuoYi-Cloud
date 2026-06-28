package com.ruoyi.patcher;

import java.io.*;
import javassist.*;

public class SingleClassPatcher {
    public static void main(String[] args) {
        String classesDirStr = "D:\\fengyong\\RuoYi-Cloud-3.6.4\\ruoyi-modules\\ruoyi-file\\target\\classes";
        String outputFileName = "SysFileController.class";

        System.out.println("Starting single class bytecode patching process...");
        System.out.println("Source Classes Dir: " + new File(classesDirStr).getAbsolutePath());
        System.out.println("Output File: " + new File(outputFileName).getAbsolutePath());

        try {
            // 1. Configure Javassist ClassPool
            System.out.println("Setting up Javassist ClassPool...");
            ClassPool pool = ClassPool.getDefault();
            
            // Append system classpath (this includes all Maven dependencies)
            pool.appendSystemPath();
            
            // Append the target classes directory
            pool.insertClassPath(classesDirStr);
            System.out.println("Added to ClassPool: " + classesDirStr);

            // 2. Load com.ruoyi.file.controller.SysFileController
            String targetClassName = "com.ruoyi.file.controller.SysFileController";
            System.out.println("Loading class: " + targetClassName);
            CtClass cc = pool.get(targetClassName);

            // 3. Modify existing upload method
            System.out.println("Finding method: upload");
            CtMethod method = cc.getDeclaredMethod("upload", new CtClass[]{
                pool.get("org.springframework.web.multipart.MultipartFile")
            });

            System.out.println("Modifying method upload body...");
            method.setBody(
                "{" +
                "    try {" +
                "        System.out.println(\"[Antigravity Patched Controller] === upload Method Started ===\");" +
                "        System.out.println(\"[Antigravity Patched Controller] File name: \" + $1.getOriginalFilename());" +
                "        String url = sysFileService.uploadFile($1);" +
                "        com.ruoyi.system.api.domain.SysFile sysFile = new com.ruoyi.system.api.domain.SysFile();" +
                "        sysFile.setName(com.ruoyi.common.core.utils.file.FileUtils.getName(url));" +
                "        sysFile.setUrl(url + \"?patched=true\");" + // Modified url
                "        System.out.println(\"[Antigravity Patched Controller] File uploaded successfully, URL: \" + url);" +
                "        return com.ruoyi.common.core.domain.R.ok(sysFile);" +
                "    } catch (Exception e) {" +
                "        log.error(\"上传文件失败\", e);" +
                "        return com.ruoyi.common.core.domain.R.fail(e.getMessage());" +
                "    }" +
                "}"
            );

            // 4. Add a new method
            System.out.println("Adding new method: antigravityHello");
            CtMethod newMethod = CtMethod.make(
                "public String antigravityHello(String name) {" +
                "    return \"Hello, \" + name + \"! This method was dynamically added via Javassist.\";" +
                "}", cc
            );
            cc.addMethod(newMethod);

            // 5. Write bytecode directly to a single file
            System.out.println("Generating bytecode and writing to file...");
            byte[] classBytes = cc.toBytecode();
            try (FileOutputStream fos = new FileOutputStream(outputFileName)) {
                fos.write(classBytes);
            }
            
            cc.detach();
            System.out.println("Single class modification completed successfully!");
            System.out.println("Output file: " + new File(outputFileName).getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Error occurred during single class patching: ");
            e.printStackTrace();
        }
    }
}
