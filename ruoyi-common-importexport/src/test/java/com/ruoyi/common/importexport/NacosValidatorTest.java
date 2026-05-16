package com.ruoyi.common.importexport;

import com.ruoyi.common.importexport.example.ExampleDTO;
import com.ruoyi.common.importexport.validation.DynamicImportValidator;
import com.ruoyi.common.importexport.validation.SpelImportValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = TestApplication.class, properties = {
        "importexport.rules.content=[{\"className\":\"com.ruoyi.common.importexport.example.ExampleDTO\",\"fieldName\":\"age\",\"required\":true,\"message\":\"Dynamic-Nacos-Injected\"}]",
        "importexport.spel-rules.content=[{\"className\":\"com.ruoyi.common.importexport.example.ExampleDTO\",\"fieldName\":\"age\",\"expression\":\"age != null && age > 0\",\"message\":\"Spel-Nacos-Injected\"}]"
})
public class NacosValidatorTest {

    @Autowired
    private DynamicImportValidator dynamicImportValidator;

    @Autowired
    private SpelImportValidator spelImportValidator;

    @BeforeEach
    public void setup() {
        // 由于测试环境中组件已被 Spring 容器实例化，
        // 且测试属性可能晚于 afterPropertiesSet 触发的初次加载，
        // 我们需要手动调用一次 loadRules 来确保读取到 mock 的 Environment 属性。
        dynamicImportValidator.loadRules();
        spelImportValidator.loadRules();
    }

    @Test
    public void testDynamicValidatorLoadsFromNacosDataId() {
        ExampleDTO dto = new ExampleDTO();
        dto.setAge(null); // 违反 required = true

        List<String> errors = dynamicImportValidator.validate(dto);

        Assertions.assertFalse(errors.isEmpty(), "校验不应为空");
        Assertions.assertTrue(errors.contains("Dynamic-Nacos-Injected"), "未找到从Nacos注入的规则消息");
    }

    @Test
    public void testSpelValidatorLoadsFromNacosDataId() {
        ExampleDTO dto = new ExampleDTO();
        dto.setAge(-1); // 违反 age > 0

        List<String> errors = spelImportValidator.validate(dto);

        Assertions.assertFalse(errors.isEmpty(), "校验不应为空");
        Assertions.assertTrue(errors.contains("Spel-Nacos-Injected"), "未找到从Nacos注入的SpEL规则消息");
    }
}
