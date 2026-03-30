package com.mhh.batch.job;

/**
 * MHH 動態背景任務接口
 */
public interface MhhJob {
    /**
     * 回傳對應資料庫 JOBS_CONF 的 JOB_NAME
     */
    String getJobName();

    /**
     * 執行任務邏輯
     */
    void execute();
}
