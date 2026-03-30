# MHH Batch_plan (排程任務建置計畫)

## 一、 技術架構 (Technical Architecture)
*   **語言與框架**: Java 21 + Spring Boot 4.0.5
*   **批次處理**: Spring Batch 5.x
*   **排程配置**: `TaskScheduler` 動態排程 (每 20 分鐘與 `JOBS_CONF` 同步)。
*   **外部設定**: `application.yml` (FTPConfig, 資料庫連線)。

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
*   `LogCleanupJob`: 每日執行，清理超過 **3 個月** 之 `JOBS_LOGS` 與超過一年之 `SYS_LOGS` 與 `USER_LOGS`。
*   **AOP 日誌攔截 (JobLoggingAspect)**:
    - 自動攔截所有 `MhhJob.execute()`。
    - 任務啟動時記錄 `START_TIME`。
    - 任務結束時自動記錄 `END_TIME`、`STATUS` (SUCCESS/FAILED) 與 `ERROR_MSG`。
    - 同步更新 `JOBS_CONF.LAST_RUN` 欄位。
    - **動態更新**: 啟動後每 20 分鐘與資料庫同步一次，動態增刪任務。
    - **Cron 組合**: 從 `JOBS_CONF` 讀取 `CRON_SEC`, `CRON_MIN`, `CRON_HOUR`, `CRON_DOM`, `CRON_MONTH`, `CRON_DOW` 欄位並組合成標準 Cron 字串。
    - 根據 `IS_ENABLED` 與 `CRON_EXPRESSION` 動態開啟/關閉或更換任務時間。
    - 不提供前端介面調整，僅由 IT 手動到 DB 更新此配置。
*   **日誌記錄**: 每個 Job 執行時均須記錄詳細歷程至 `JOBS_LOGS` 表。

### 2.3 排程與重試機制
*   **定時觸發**:
    - `JobRefresher`: 啟動時立即執行，隨後每 20 分鐘執行一次。
    - 其他所有任務（`SwallowSyncJob`, `HrSyncJob`, `PdfImportJob`, `ReservationMergeJob`, `LogCleanupJob`）之 Cron 一律從資料庫動態獲取。
*   **錯誤重試**: 使用 Spring Retry 實作 FTP 連線重試與資料庫存取失敗重試。

## 三、 系統執行流程 (Execution Flow)

系統啟動後，排程任務之運作遵循以下流程：

1.  **同步設定 (Sync Settings)**:
    - 啟動時立即執行第一次同步，隨後每 20 分鐘 (由 `mhh.jobs.refresh-rate` 控制) 掃描一次資料庫 `JOBS_CONF` 表。
2.  **組合 Cron 表達式 (Assemble Cron)**:
    - 從 `CRON_SEC` 到 `CRON_DOW` 讀取 6 個結構化欄位，並組合法規之標準 Cron 字串。
3.  **動態註冊與更新 (Schedule/Register)**:
    - `DynamicJobService` 比對 Cron 是否變動。
    - 若有變動，則透過 `TaskScheduler` 取消舊任務並重新註冊。
4.  **任務執行 (Job Execution)**:
    - 到達觸發時間時，系統透過 `getJobName()` 尋找對應的 `MhhJob` Bean 並執行其 `execute()` 方法。
5.  **AOP 自動日誌記錄 (AOP Logging)**:
    - `JobLoggingAspect` 自動記錄執行歷程至 `JOBS_LOGS` 並更新 `JOBS_CONF.LAST_RUN`。
