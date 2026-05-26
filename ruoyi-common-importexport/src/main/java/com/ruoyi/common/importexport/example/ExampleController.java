package com.ruoyi.common.importexport.example;

import com.ruoyi.common.importexport.dto.ImportResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 示例 Controller
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/example/file")
public class ExampleController {

    @Autowired
    private ExampleService exampleService;

    /**
     * 通用导出
     */
    @GetMapping("/export/{fileType}")
    public ResponseEntity<StreamingResponseBody> exportData(@PathVariable("fileType") String fileType) {
        // 模拟数据
        List<ExampleDTO> list = new ArrayList<>();
        ExampleDTO dto1 = new ExampleDTO();
        dto1.setId(1L);
        dto1.setUsername("张三");
        dto1.setAge(20);
        list.add(dto1);

        StreamingResponseBody stream = out -> {
            exampleService.exportData(fileType, list, out);
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=export." + fileType.toLowerCase())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(stream);
    }

    /**
     * 通用多Sheet/兼容导出
     */
    @GetMapping("/export-multi/{fileType}")
    public ResponseEntity<StreamingResponseBody> exportMultiSheetData(@PathVariable("fileType") String fileType) {
        // 模拟数据
        Map<String, List<ExampleDTO>> sheetMap = new LinkedHashMap<>();

        List<ExampleDTO> list1 = new ArrayList<>();
        ExampleDTO dto1 = new ExampleDTO();
        dto1.setId(1L);
        dto1.setUsername("张三");
        dto1.setAge(20);
        list1.add(dto1);
        sheetMap.put("部门A", list1);

        List<ExampleDTO> list2 = new ArrayList<>();
        ExampleDTO dto2 = new ExampleDTO();
        dto2.setId(2L);
        dto2.setUsername("李四");
        dto2.setAge(25);
        list2.add(dto2);
        sheetMap.put("部门B", list2);

        StreamingResponseBody stream = out -> {
            exampleService.exportMultiSheetData(fileType, sheetMap, out);
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=export-multi." + fileType.toLowerCase())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(stream);
    }

    /**
     * 通用导入
     */
    @PostMapping("/import/{fileType}")
    public Object importData(@PathVariable("fileType") String fileType,
                             @RequestParam("file") MultipartFile file,
                             @RequestParam(value = "engine", required = false, defaultValue = "regex") String engine) throws Exception {
        ImportResult<ExampleDTO> result = exampleService.importData(fileType, file.getInputStream(), engine);

        if (result.isSuccess()) {
            return "导入成功，共处理 " + result.getSuccessList().size() + " 条数据";
        } else {
            return "导入存在错误: " + result.getErrorMessages();
        }
    }
}
