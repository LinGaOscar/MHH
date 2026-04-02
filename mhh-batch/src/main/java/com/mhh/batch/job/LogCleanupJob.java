package com.mhh.batch.job;

import com.mhh.common.repository.UserLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component("LogCleanupJob")
@RequiredArgsConstructor
public class LogCleanupJob implements MhhJob {

    private final UserLogRepository userLogRepository;

    @Override
    public String getJobName() {
        return "LogCleanupJob";
    }

    @Override
    @Transactional
    public void execute() {
        log.info("[LogCleanupJob] 啟動 - 清理超過 1 年之 USER_LOGS...");

        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        userLogRepository.deleteOlderThan(oneYearAgo);

        // SYS_LOG / BATCH_LOG 以本機檔案儲存，由 logback-spring.xml 的
        // TimeBasedRollingPolicy 自動滾動與保留期限管理，無需程式介入清理
        log.info("[LogCleanupJob] 完成");
    }
}
