package com.mhh.batch.config;

import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;

/**
 * Configuration for the secondary Oracle database (SWAL).
 */
@Configuration
public class ExternalDbConfig {

    @Bean
    @ConfigurationProperties("mhh.swal.datasource")
    public DataSourceProperties swalDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "swalDataSource")
    public DataSource swalDataSource() {
        return swalDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean(name = "swalJdbcTemplate")
    public JdbcTemplate swalJdbcTemplate() {
        return new JdbcTemplate(swalDataSource());
    }
}
