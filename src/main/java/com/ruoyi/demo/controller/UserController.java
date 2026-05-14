package com.ruoyi.demo.controller;

import com.ruoyi.common.importexport.dto.ImportResult;
import com.ruoyi.common.importexport.enums.FileTypeEnum;
import com.ruoyi.demo.dto.UserDTO;
import com.ruoyi.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/import")
    public ImportResult<UserDTO> importData(@RequestParam("file") MultipartFile file,
                                            @RequestParam("type") FileTypeEnum type) throws Exception {
        return userService.importUsers(file.getInputStream(), type);
    }

    @GetMapping("/export")
    public void exportData(@RequestParam("type") FileTypeEnum type, HttpServletResponse response) throws Exception {
        response.setContentType("application/octet-stream");
        response.setHeader("Content-disposition", "attachment;filename=users." + type.getType());

        List<UserDTO> users = new ArrayList<>();
        userService.exportUsers(users, type, response.getOutputStream());
    }
}
