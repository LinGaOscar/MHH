package com.mhh.ap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.mhh.ap", "com.mhh.core", "com.mhh.common"})
public class MhhApApplication {
    public static void main(String[] args) {
        SpringApplication.run(MhhApApplication.class, args);
    }
}
