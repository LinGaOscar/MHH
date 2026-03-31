# IT 運維手冊 (Operations Manual)

本文件適用對象：**IT 維運人員**。  
涵蓋排程任務管理、日誌管理、資料清理策略與常見問題。

> 部署與啟動步驟請參閱 → [快速啟動指南 Part B](2_quickstart.md#part-b生產環境部署)

---

## 1. 排程任務管理 (Spring Batch)

### 1.1 任務清單

| Job 名稱 | 說明 | 預設頻率 |
|:--|:--|:--|
| `SwallowSyncJob` | 連線外部 SWAL (Oracle)，同步電文數據至 `MSG_SWAL_SYNC` | 由 DB 動態配置 |
| `PdfImportJob` | 掃描 `\data\MX` 與 `\data\MT`，解析 PDF 並匯入 `MSG_HISTORY` | 由 DB 動態配置 |
| `HrSyncJob` | 同步員工在職狀態至 `HR_USER` 與 `HR_UNIT` | 由 DB 動態配置 |
| `ReservationMergeJob` | 執行使用者預約之多筆 PDF 合併，結果暫存至 `\data\TEMP\` | 由 DB 動態配置 |
| `LogCleanupJob` | 清理過期日誌（`JOBS_LOGS` 保留 3 個月，`SYS_LOGS` 與 `USER_LOGS` 保留 1 年） | 每日執行 |
| **`JobRefresher`** | 動態同步 `JOBS_CONF` 排程配置，並重新註冊變動的 Job | 啟動時立刻執行，之後每 20 分鐘 |

### 1.2 動態排程調整

IT 人員可**直接修改 `JOBS_CONF` 資料表**來調整排程，無需重新部署：

| 欄位 | 說明 |
|:--|:--|
| `CRON_SEC` ~ `CRON_DOW` | 6 個欄位組合成標準 Cron 字串（秒/分/時/日/月/週） |
| `IS_ENABLED` | `1` = 啟用，`0` = 停用此 Job |
| `LAST_RUN` | 由系統自動更新，記錄最後執行時間（請勿手動修改） |

> `JobRefresher` 每 20 分鐘自動抓取最新設定後動態更新排程，**修改後無需重啟服務**。

---

## 2. 日誌管理

本系統所有業務日誌皆儲存於 MHH 資料庫，不依賴本機檔案稽核：

| 資料表 | 說明 | 保留期限 |
|:--|:--|:--|
| `SYS_LOGS` | 系統 Exception、連線異常、AOP 攔截到的執行錯誤 | **1 年** |
| `USER_LOGS` | 使用者行為稽核：查詢、下載、申請、放行、權限異動 | **1 年** |
| `JOBS_LOGS` | 所有排程 Job 執行歷程：Start/End Time、Status、Error Stack Trace | **3 個月** |

> `LogCleanupJob` 每日自動清理過期紀錄，確保資料庫不會無限增長。

---

## 3. 資料清理策略

| 資料類型 | 清理規則 |
|:--|:--|
| 合併後 PDF 實體檔案（`\data\TEMP\`） | `MSG_DOWNLOAD.EXPIRY_DATE` 到期後由 IT 手動清理或排程刪除 |
| 已解析歸檔的 PDF（`\data\ARCHIVE\`） | 依業務保存年限由 IT 定期清理 |
| `SYS_LOGS` / `USER_LOGS` | `LogCleanupJob` 自動清理超過 1 年的紀錄 |
| `JOBS_LOGS` | `LogCleanupJob` 自動清理超過 3 個月的紀錄 |

---

## 4. 監控建議

- **Job 狀態監控**：定期查詢 `JOBS_LOGS` 中 `STATUS = 'FAILED'` 的紀錄，確認是否有長期失敗的排程。
- **磁碟空間**：定期確認 `\data\ARCHIVE\` 與 `\data\TEMP\` 的磁碟使用量。
- **資料庫連線**：若 `SwallowSyncJob` 持續失敗，優先檢查 SWAL Oracle 連線憑證是否過期。

---

## 5. 常見問題 (Troubleshooting)

| 問題 | 可能原因 | 解決方式 |
|:--|:--|:--|
| 排程任務停止執行 | `IS_ENABLED = 0` 或 Cron 格式錯誤 | 至 `JOBS_CONF` 確認欄位設定 |
| PDF 匯入失敗 | 來源目錄權限不足 | 確認執行帳戶對 `\data` 有讀寫刪除權限 |
| SWAL 同步失敗 | Oracle 連線參數錯誤或網路不通 | 確認 `application.yml` 中的 SWAL DataSource 設定 |
| 使用者無法登入 | SWALLOW 權限未設定或 HR 狀態異常 | 至 `USER` 及 `HR_USER` 資料表確認帳號狀態 |
| `\data\TEMP\` 磁碟滿 | 過期合併檔未清理 | 手動清理已過 `EXPIRY_DATE` 的暫存檔案 |
