package com.ruoyi.common.importexport.example;

import com.ruoyi.common.importexport.dto.ImportResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
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
     * 大数据量导出 Demo (使用函数式接口分页查询)
     */
    @GetMapping("/export-big-data/{fileType}")
    public void exportBigData(@PathVariable("fileType") String fileType, HttpServletResponse response) throws Exception {
        response.setContentType("application/octet-stream;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=export-big-data." + fileType.toLowerCase());

        // 模拟业务分页查询逻辑 (实际项目中这里应该是 service.selectPage(...) 或 mapper.selectList(...))
        int totalRecords = 55; // 假设总共有55条数据
        int pageSize = 10;     // 每页查询10条

        java.util.function.Function<Integer, List<ExampleDTO>> pageDataLoader = pageNum -> {
            int start = (pageNum - 1) * pageSize;
            if (start >= totalRecords) {
                return null; // 返回null或空集合表示数据读取完毕
            }
            int end = Math.min(start + pageSize, totalRecords);

            List<ExampleDTO> list = new ArrayList<>();
            for (int i = start; i < end; i++) {
                ExampleDTO dto = new ExampleDTO();
                dto.setId((long) (i + 1));
                dto.setUsername("User_" + (i + 1));
                dto.setAge(20 + (i % 30)); // 随机年龄
                dto.setRemark("第 " + pageNum + " 页的数据");
                list.add(dto);
            }
            // 模拟数据库查询耗时
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            return list;
        };

        exampleService.exportBigData(fileType, pageDataLoader, response.getOutputStream());
    }

    /**
     * 通用多Sheet/兼容导出
     */
    @GetMapping("/export-multi/{fileType}")
    public void exportMultiSheetData(@PathVariable("fileType") String fileType, HttpServletResponse response) throws Exception {
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

        response.setContentType("application/octet-stream;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=export-multi." + fileType.toLowerCase());

        exampleService.exportMultiSheetData(fileType, sheetMap, response.getOutputStream());
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
