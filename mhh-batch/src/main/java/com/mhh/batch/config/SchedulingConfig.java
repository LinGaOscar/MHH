package com.mhh.batch.config;

import com.mhh.batch.service.JobSyncScheduler;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulingConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5); // 預設 5 個線程執行背景任務
        scheduler.setThreadNamePrefix("mhh-job-");
        scheduler.initialize();
        return scheduler;
    }

    /**
     * 啟動後立即執行第一次同步，而不必等待 20 分鐘
     */
    @Bean
    public CommandLineRunner initialJobSync(JobSyncScheduler jobSyncScheduler) {
        return args -> jobSyncScheduler.syncJobs();
    }
}
