package com.mhh.batch.job;

import com.mhh.batch.repository.JobLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component("LogCleanupJob")
public class LogCleanupJob implements MhhJob {

    private final JobLogRepository jobLogRepository;

    public LogCleanupJob(JobLogRepository jobLogRepository) {
        this.jobLogRepository = jobLogRepository;
    }

    @Override
    public String getJobName() {
        return "LogCleanupJob";
    }

    @Override
    public void execute() {
        log.info("[LogCleanupJob] 啟動 - 正在清理超過 3 個月之 JOBS_LOGS 與超過 1 年之系統日誌...");
        try {
            LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
            jobLogRepository.deleteOlderThan(threeMonthsAgo);
            
            log.info("[LogCleanupJob] 清理完成");
        } catch (Exception e) {
            log.error("[LogCleanupJob] 清理失敗: {}", e.getMessage());
        }
    }
}
