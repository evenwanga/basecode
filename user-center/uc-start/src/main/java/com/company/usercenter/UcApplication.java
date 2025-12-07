package com.company.usercenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = "com.company")
@EnableJpaRepositories(basePackages = "com.company")
public class UcApplication {

    public static void main(String[] args) {
        SpringApplication.run(UcApplication.class, args);
    }
}
