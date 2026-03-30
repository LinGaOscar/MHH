package com.mhh.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.mhh.batch", "com.mhh.core", "com.mhh.common"})
public class MhhBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(MhhBatchApplication.class, args);
    }
}
