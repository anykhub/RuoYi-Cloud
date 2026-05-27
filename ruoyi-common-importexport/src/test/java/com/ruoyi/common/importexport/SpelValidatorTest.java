package com.ruoyi.common.importexport;

import com.ruoyi.common.importexport.example.ExampleDTO;
import com.ruoyi.common.importexport.example.ExampleDetailDTO;
import com.ruoyi.common.importexport.validation.SpelImportValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = TestApplication.class)
public class SpelValidatorTest {

    @Autowired
    private SpelImportValidator spelImportValidator;

    @BeforeEach
    public void setup() {
        spelImportValidator.loadRules();
    }

    @Test
    public void testUsernameLengthValidation_TooShort() {
        ExampleDTO dto = new ExampleDTO();
        dto.setAge(25);
        ExampleDetailDTO detail = new ExampleDetailDTO();
        detail.setDescription("Valid desc");
        dto.setDetail(detail);
        dto.setUsername("a"); // length < 2

        List<String> errors = spelImportValidator.validate(dto);

        Assertions.assertTrue(errors.contains("[SpEL] 用户名长度必须在2到20个字符之间"), "应当包含长度校验错误");
    }

    @Test
    public void testUsernameRegexValidation_InvalidChars() {
        ExampleDTO dto = new ExampleDTO();
        dto.setAge(25);
        ExampleDetailDTO detail = new ExampleDetailDTO();
        detail.setDescription("Valid desc");
        dto.setDetail(detail);
        dto.setUsername("invalid-name!"); // invalid characters

        List<String> errors = spelImportValidator.validate(dto);

        Assertions.assertTrue(errors.contains("[SpEL] 用户名只能包含字母、数字和下划线"), "应当包含正则校验错误");
    }

    @Test
    public void testUsernameValid() {
        ExampleDTO dto = new ExampleDTO();
        dto.setAge(25);
        ExampleDetailDTO detail = new ExampleDetailDTO();
        detail.setDescription("Valid desc");
        dto.setDetail(detail);
        dto.setUsername("valid_user_123");

        List<String> errors = spelImportValidator.validate(dto);

        Assertions.assertTrue(errors.isEmpty(), "校验应该通过");
    }
}
