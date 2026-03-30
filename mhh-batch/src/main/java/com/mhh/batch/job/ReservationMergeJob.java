package com.mhh.batch.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("ReservationMergeJob") // Bean 名稱需與資料庫 JOB_NAME 匹配
public class ReservationMergeJob implements MhhJob {

    @Override
    public String getJobName() {
        return "ReservationMergeJob";
    }

    @Override
    public void execute() {
        log.info("[ReservationMergeJob] 啟動 - 正在處理放行之電文預約下載與 PDF 合併...");
        try {
            // TODO: 實作 PDF 合併邏輯
            log.info("[ReservationMergeJob] 處理完成");
        } catch (Exception e) {
            log.error("[ReservationMergeJob] 處理失敗: {}", e.getMessage());
        }
    }
}
