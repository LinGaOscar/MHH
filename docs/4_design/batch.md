# Batch 模組設計規格 (Batch Module Design)

← [返回設計文件導覽](README.md)

---

## 1. 技術架構

| 項目 | 規格 |
|:--|:--|
| 語言與框架 | Java 21 + Spring Boot 4.0.5 |
| 批次處理 | Spring Batch 5.x |
| 排程機制 | `TaskScheduler` 動態排程（每 20 分鐘與 `JOBS_CONF` 同步） |
| 模組依賴 | 強制依賴 `mhh-common`（與 AP 共用所有 Entity、Repository、工具類） |
| 外部設定 | `application.yml`（FTPConfig、資料庫連線） |

---

## 2. Job 設計

### 2.1 核心 Job 清單

| Job | 說明 | 資料流向 |
|:--|:--|:--|
| `SwalSyncJob` | 從外部 Oracle SWAL 讀取電文 | → `MSG_INCOMING` |
| `PdfImportJob` | 掃描 MX/MT 目錄，解析原始 PDF 並匯入 | 進電 → `MSG_INCOMING`；出電 → `MSG_OUTGOING`；PDF → `\data\ARCHIVE\` |
| `HrSyncJob` | 同步員工在職狀態 | → `HR_USER`、`HR_UNIT` |
| `ReservationMergeJob` | 執行使用者預約的多筆 PDF 合併 | → `\data\TEMP\`；狀態更新 `MSG_DOWNLOAD` |
| `LogCleanupJob` | 清理過期資料（每日執行） | 刪除 `USER_LOGS` 1 年以上紀錄；本機 `batch.log`/`sys.log` 由 Logback 滾動策略自動管理 |
| `JobRefresher` | 動態同步 `JOBS_CONF`，更新執行中的排程 | — |

### 2.2 AOP 自動日誌 (JobLoggingAspect)

所有 Job 無需手動埋點，由 AOP 自動處理：

- 自動攔截所有 `MhhJob.execute()` 方法
- 使用專用 logger `BATCH_LOG` 寫入本機 `batch.log`（由 `logback-spring.xml` 設定）
- 任務啟動時記錄 `jobName`、`startTime`
- 任務完成時記錄 `endTime`、`status`（`SUCCESS` / `FAILED`）、`error`（如有）
- 同步更新 `JOBS_CONF.LAST_RUN` 欄位

---

## 3. 資料存取策略

| 場景 | 策略 |
|:--|:--|
| 大批量讀取 | `JdbcPagingItemReader`（Native SQL，避免 OOM） |
| 大批量寫入 | `JdbcBatchItemWriter`（批次 INSERT/UPDATE） |
| 簡單狀態更新 | 共用 JPA Repository（`spring-data-jpa`） |

---

## 4. 動態排程機制

### 4.1 Cron 欄位規則

`JOBS_CONF` 資料表以 6 個獨立欄位儲存 Cron 設定，啟動時由系統自動組合：

```
CRON_EXPRESSION = CRON_SEC + " " + CRON_MIN + " " + CRON_HOUR + " " + CRON_DOM + " " + CRON_MONTH + " " + CRON_DOW
```

### 4.2 動態更新流程

```
系統啟動
   │
   ├─→ JobRefresher 立即執行
   │       │
   │       ├─ 讀取 JOBS_CONF（IS_ENABLED, CRON_*)
   │       ├─ 組合 Cron 字串
   │       └─ DynamicJobService 比對變動
   │              │
   │              ├─ [有變動] 取消舊 Task → 重新 Schedule
   │              └─ [無變動] 略過
   │
   └─→ 每 20 分鐘重複上述流程
```

> **關鍵**：IT 人員直接修改 `JOBS_CONF.IS_ENABLED` 或 Cron 欄位，最多 20 分鐘後自動生效，**無需重啟服務**。

---

## 5. 錯誤重試機制

- 使用 **Spring Retry** 實作 FTP 連線重試與資料庫存取失敗重試
- 重試次數與間隔可透過 `application.yml` 設定

---

## 6. 建置進度

- [x] **Step 1**：專案初始化與模組結構（依賴 `mhh-common`）
- [x] **Step 2**：動態排程框架（`DynamicJobService`、`JobRefresher`）
- [x] **Step 3**：`SwalSyncJob` 實作（SWAL → `MSG_INCOMING`）
- [x] **Step 4**：`PdfImportJob` + 策略模式 Parser 實作（`Mt103Parser`、`MtGenericParser`、`MxGenericParser`）
- [x] **Step 5**：`LogCleanupJob`（清 `USER_LOGS`）；本機日誌由 `logback-spring.xml` 管理
