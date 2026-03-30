package com.mhh.batch.service;

import com.mhh.batch.repository.JobConfRepository;
import com.mhh.common.entity.JobConf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class JobSyncScheduler {

    private final JobConfRepository jobConfRepository;
    private final DynamicJobService dynamicJobService;

    public JobSyncScheduler(JobConfRepository jobConfRepository, DynamicJobService dynamicJobService) {
        this.jobConfRepository = jobConfRepository;
        this.dynamicJobService = dynamicJobService;
    }

    /**
     * 定期同步資料庫中的 Job 配置
     * 預設每 20 分鐘執行一次 (依據 application.yml 配置)
     */
    @Scheduled(fixedRateString = "${mhh.jobs.refresh-rate:20m}")
    public void syncJobs() {
        log.info("開始同步資料庫排程配置...");
        try {
            List<JobConf> allJobs = jobConfRepository.findAll();
            allJobs.forEach(dynamicJobService::upsertJob);
            log.info("排程配置同步完成，共處理 {} 個任務", allJobs.size());
        } catch (Exception e) {
            log.error("同步排程配置失敗: {}", e.getMessage());
        }
    }
}
