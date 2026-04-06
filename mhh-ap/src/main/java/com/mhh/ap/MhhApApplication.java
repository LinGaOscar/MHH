package com.mhh.ap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ComponentScan(basePackages = {"com.mhh.ap", "com.mhh.core", "com.mhh.common"})
@EnableJpaRepositories(basePackages = {"com.mhh.ap", "com.mhh.common"})
@EntityScan(basePackages = {"com.mhh.ap", "com.mhh.common"})
@EnableAsync
public class MhhApApplication {
    public static void main(String[] args) {
        SpringApplication.run(MhhApApplication.class, args);
    }
}
