# MHH Batch_plan (排程任務建置計畫)

## 一、 技術架構 (Technical Architecture)
*   **語言與框架**: Java 21 + Spring Boot 4.0.5
*   **批次處理**: Spring Batch 5.x
*   **排程配置**: @Scheduled + Cron 排程描述。
*   **外部設定**: `application.yml` (FTPConfig, Cron 參數)。

## 二、 系統規劃 (System Planning)
### 2.1 專案結構
*   `mhh-worker`: 獨立建置的執行的 Jar，與 `mhh-api` 分開。
*   **模組重用**: 核心強制依賴 `mhh-common`，與 AP 共用所有 Entity、Repository 與共用工具類 (Common Tools)，確保商業邏輯一致。
*   **資料存取策略**:
    - 大數據讀取/批次寫入: 使用 Native SQL (`JdbcPagingItemReader`, `JdbcBatchItemWriter`)。
    - 簡單狀態更新: 使用共用的 JPA Repository。

### 2.2 核心 Job 設計
*   `SwallowSyncJob`: 從外部 FTP 擷取資料，並與 `MSG_SWAL_SYNC` 暫存表對應。
*   `HrSyncJob`: 同步員工在職狀態，更新 `HR_USER` 與 `HR_UNIT`。
*   `PdfImportJob`: 掃描 FTP，將原始 PDF 匯入系統並更新 `MSG_HISTORY`。
*   `ReservationMergeJob`: 針對已放行的下載預約，進行實體 PDF 檔案合併與準備。

### 2.3 排程與重試機制
*   **定時觸發**:
    - `SwallowSyncJob`: 每日 01:00。
    - `HrSyncJob`: 每日 02:00。
    - `PdfImportJob`: 每日 23:00。
    - `ReservationMergeJob`: 每 5 分鐘執行一次 (近即時合併)。
*   **錯誤重試**: 使用 Spring Retry 實作 FTP 連線重試與資料庫存取失敗重試。

## 三、 建置步驟 (Building Steps)
- [ ] **Step 1: Batch 模組初始化**
    - 建立 `mhh-worker` 模組專案。
    - 配置 `mhh.jobs.cron` 屬性。
- [ ] **Step 2: 基礎 Job 開發**
    - 實作 `SwallowSyncJob` (FTPReader -> Writer)。
    - 實作 `HrSyncJob`。
- [ ] **Step 3: PDF 處理 Jobs**
    - 實作 `PdfImportJob` (掃描與多執行緒匯入)。
    - 實作 `ReservationMergeJob` (呼叫 `IPdfEngineService` 合併多筆 PDF)。
- [ ] **Step 4: 排程管理與測試**
    - 使用 `@Scheduled` 管理各任務啟動時間。
    - 確保執行結果記錄到 `JOBS_LOGS`。
