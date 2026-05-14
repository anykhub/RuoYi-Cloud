package com.ruoyi.common.importexport.example;

import com.ruoyi.common.importexport.dto.ImportResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

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
    public void exportData(@PathVariable("fileType") String fileType, HttpServletResponse response) throws Exception {
        // 模拟数据
        List<ExampleDTO> list = new ArrayList<>();
        ExampleDTO dto1 = new ExampleDTO();
        dto1.setId(1L);
        dto1.setUsername("张三");
        dto1.setAge(20);
        list.add(dto1);

        response.setContentType("application/octet-stream;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=export." + fileType.toLowerCase());

        exampleService.exportData(fileType, list, response.getOutputStream());
    }

    /**
     * 通用导入
     */
    @PostMapping("/import/{fileType}")
    public Object importData(@PathVariable("fileType") String fileType, @RequestParam("file") MultipartFile file) throws Exception {
        ImportResult<ExampleDTO> result = exampleService.importData(fileType, file.getInputStream());

        if (result.isSuccess()) {
            return "导入成功，共处理 " + result.getSuccessList().size() + " 条数据";
        } else {
            return "导入存在错误: " + result.getErrorMessages();
        }
    }
}
