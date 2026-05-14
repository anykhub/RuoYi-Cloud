package com.ruoyi.demo.service;

import com.ruoyi.common.importexport.core.ImportExportHandler;
import com.ruoyi.common.importexport.dto.ImportResult;
import com.ruoyi.common.importexport.enums.FileTypeEnum;
import com.ruoyi.common.importexport.factory.FileHandlerFactory;
import com.ruoyi.common.importexport.validator.ImportValidator;
import com.ruoyi.demo.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Service
public class UserService {
    @Autowired
    private FileHandlerFactory factory;

    @Autowired
    private ImportValidator validator;

    public void exportUsers(List<UserDTO> users, FileTypeEnum type, OutputStream os) {
        ImportExportHandler<UserDTO> handler = factory.getHandler(type);
        handler.exportData(users, os);
    }

    public ImportResult<UserDTO> importUsers(InputStream is, FileTypeEnum type) {
        ImportExportHandler<UserDTO> handler = factory.getHandler(type);
        List<UserDTO> data = handler.importData(is, UserDTO.class);
        return validator.validate(data);
    }
}
