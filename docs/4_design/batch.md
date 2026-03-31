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
| `SwallowSyncJob` | 從外部 FTP 擷取 SWAL 資料 | → `MSG_SWAL_SYNC`（暫存）→ `MSG_HISTORY` |
| `PdfImportJob` | 掃描 FTP，解析原始 PDF 並匯入 | → `MSG_HISTORY`；PDF → `\data\ARCHIVE\` |
| `HrSyncJob` | 同步員工在職狀態 | → `HR_USER`、`HR_UNIT` |
| `ReservationMergeJob` | 執行使用者預約的多筆 PDF 合併 | → `\data\TEMP\`；狀態更新 `MSG_DOWNLOAD` |
| `LogCleanupJob` | 清理過期日誌（每日執行） | 刪除 `JOBS_LOGS`（3 個月）、`SYS_LOGS` / `USER_LOGS`（1 年）過期紀錄 |
| `JobRefresher` | 動態同步 `JOBS_CONF`，更新執行中的排程 | — |

### 2.2 AOP 自動日誌 (JobLoggingAspect)

所有 Job 無需手動埋點，由 AOP 自動處理：

- 自動攔截所有 `MhhJob.execute()` 方法
- 任務啟動時記錄 `START_TIME`
- 任務完成時記錄 `END_TIME`、`STATUS`（`SUCCESS` / `FAILED`）、`ERROR_MSG`（如有）
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
- [ ] **Step 2**：動態排程框架（`DynamicJobService`、`JobRefresher`）
- [ ] **Step 3**：`SwallowSyncJob` 實作（SWAL → `MSG_SWAL_SYNC`）
- [ ] **Step 4**：`PdfImportJob` + 策略模式 Parser 實作（含 MT103 範例）
- [ ] **Step 5**：`LogCleanupJob` + `ReservationMergeJob` 實作
