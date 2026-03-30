package com.mhh.batch.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("SwallowSyncJob")
class SwallowSyncJob implements MhhJob {
    @Override public String getJobName() { return "SwallowSyncJob"; }
    @Override public void execute() { log.info("[SwallowSyncJob] 啟動 - 正在同步 SWALLOW 電文資料..."); }
}

@Slf4j
@Component("HrSyncJob")
class HrSyncJob implements MhhJob {
    @Override public String getJobName() { return "HrSyncJob"; }
    @Override public void execute() { log.info("[HrSyncJob] 啟動 - 正在同步 HR 員工狀態..."); }
}

@Slf4j
@Component("PdfImportJob")
class PdfImportJob implements MhhJob {
    @Override public String getJobName() { return "PdfImportJob"; }
    @Override public void execute() { log.info("[PdfImportJob] 啟動 - 正在掃描並匯入原始 PDF 檔案..."); }
}
