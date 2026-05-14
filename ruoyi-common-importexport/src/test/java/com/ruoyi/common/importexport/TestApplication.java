package com.ruoyi.common.importexport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.context.annotation.Bean;
import javax.validation.Validator;

/**
 * 测试专用的最小化 Spring Boot 启动类
 */
@SpringBootApplication
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

    @Bean
    public Validator validator() {
        return new LocalValidatorFactoryBean();
    }
}
