package com.ruoyi.common.importexport;

import com.ruoyi.common.importexport.dto.ImportResult;
import com.ruoyi.common.importexport.enums.FileTypeEnum;
import com.ruoyi.common.importexport.example.ExampleDTO;
import com.ruoyi.common.importexport.example.ExampleService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 导入导出组件的单元测试与使用示例
 *
 * 展示了如何对 Excel, CSV, JSON, XML, TXT 五种格式进行导出和导入操作。
 */
@Slf4j
@SpringBootTest(classes = TestApplication.class)
public class ImportExportTest {

    @Autowired
    private ExampleService exampleService;

    private List<ExampleDTO> testData;

    @BeforeEach
    public void setup() {
        // 构造测试数据
        testData = new ArrayList<>();

        ExampleDTO dto1 = new ExampleDTO();
        dto1.setId(1L);
        dto1.setUsername("张三");
        dto1.setAge(25);
        dto1.setRemark("测试人员1");
        testData.add(dto1);

        ExampleDTO dto2 = new ExampleDTO();
        dto2.setId(2L);
        dto2.setUsername("李四");
        dto2.setAge(30);
        dto2.setRemark("测试人员2");
        testData.add(dto2);

        // 构造一条会导致校验失败的数据 (年龄为空)
        ExampleDTO dto3 = new ExampleDTO();
        dto3.setId(3L);
        dto3.setUsername("王五");
        dto3.setAge(null); // 年龄为 @NotNull
        dto3.setRemark("异常数据测试");
        testData.add(dto3);
    }

    /**
     * 测试并演示 Excel 的导入导出
     */
    @Test
    public void testExcelImportExport() throws Exception {
        log.info("--- 开始测试 Excel ---");
        testType(FileTypeEnum.EXCEL.getCode());
    }

    /**
     * 测试并演示 CSV 的导入导出
     */
    @Test
    public void testCsvImportExport() throws Exception {
        log.info("--- 开始测试 CSV ---");
        testType(FileTypeEnum.CSV.getCode());
    }

    /**
     * 测试并演示 JSON 的导入导出
     */
    @Test
    public void testJsonImportExport() throws Exception {
        log.info("--- 开始测试 JSON ---");
        testType(FileTypeEnum.JSON.getCode());
    }

    /**
     * 测试并演示 XML 的导入导出
     */
    @Test
    public void testXmlImportExport() throws Exception {
        log.info("--- 开始测试 XML ---");
        testType(FileTypeEnum.XML.getCode());
    }

    /**
     * 测试并演示 TXT 的导入导出
     */
    @Test
    public void testTxtImportExport() throws Exception {
        log.info("--- 开始测试 TXT ---");
        testType(FileTypeEnum.TXT.getCode());
    }

    /**
     * 通用的导出和导入测试流程
     *
     * @param fileType 文件类型
     */
    private void testType(String fileType) throws Exception {
        // 1. 测试导出到内存流 (实际生产中这里往往是 HttpServletResponse 的 OutputStream)
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        exampleService.exportData(fileType, testData, os);

        byte[] fileBytes = os.toByteArray();
        Assertions.assertTrue(fileBytes.length > 0, fileType + " 导出失败，数据为空");
        log.info("{} 导出成功，文件字节数: {}", fileType, fileBytes.length);

        // 2. 测试从内存流导入 (实际生产中这里往往是 MultipartFile.getInputStream())
        InputStream is = new ByteArrayInputStream(fileBytes);
        ImportResult<ExampleDTO> result = exampleService.importData(fileType, is);

        // 3. 验证导入结果
        // 因为我们放入了 3 条数据，其中 1 条故意缺少必填项，所以成功应该有 2 条，失败有 1 条
        log.info("{} 导入完成. 成功数量: {}, 失败数量: {}",
                fileType, result.getSuccessList().size(), result.getErrorMessages().size());

        if (!result.getErrorMessages().isEmpty()) {
            log.info("{} 校验错误信息: {}", fileType, result.getErrorMessages().get(0));
        }

        Assertions.assertEquals(2, result.getSuccessList().size(), fileType + " 成功数据量不符");
        Assertions.assertEquals(1, result.getErrorMessages().size(), fileType + " 失败数据量不符");

        // 验证第一条成功数据的字段是否匹配
        ExampleDTO firstImported = result.getSuccessList().get(0);
        Assertions.assertEquals("张三", firstImported.getUsername(), "数据内容读取错误");
    }
}
