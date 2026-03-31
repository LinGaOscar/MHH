# 資料庫設計規格 (Database Design)

← [返回設計文件導覽](README.md)

---

## 1. 技術架構

| 項目 | 規格 |
|:--|:--|
| 資料庫引擎 | MS SQL Server 2019+ |
| 字元集 | `UTF-8`（或資料庫對應之 N-charset） |
| ORM 框架 | Hibernate 6.x（Spring Data JPA） |
| 外部設定 | `application.yml`（DataSource） |
| SQL 策略 | **不使用預存程序**，複雜 SQL 集中於 Java 代碼中實作 |

---

## 2. 資料表清單

### 2.1 使用者與權限 (User & Auth)

| 資料表 | 說明 |
|:--|:--|
| `USER` | 使用者基礎帳號表 |
| `USER_ROLE` | 使用者自訂角色與權限映射（含 `BRANCH_MAKER`、`BRANCH_CHECKER`、`PARAM_MAKER`、`PARAM_CHECKER`） |
| `USER_CUST` | 自定義使用者屬性配置表格 |

### 2.2 HR 同步 (HR Sync)

| 資料表 | 說明 |
|:--|:--|
| `HR_USER` | 員工基本資料同步表 |
| `HR_UNIT` | 組織單位架構同步表 |

### 2.3 電文業務 (Message Business — MSG_ 開頭)

| 資料表 | 說明 |
|:--|:--|
| `MSG_HISTORY` | SWIFT 電文歷史主表 |
| `MSG_DOWNLOAD` | 預約下載詳情與合併狀態（含 `EXPIRY_DATE`、`FILE_SIZE`）；單筆免放行，多筆需預約 |
| `MSG_APPROVAL` | 工作流審核任務表 |
| `MSG_SWAL_SYNC` | SWALLOW 原始資料同步至本系統之暫存表 |

### 2.4 系統監控 (System & Monitoring)

| 資料表 | 說明 | 保留期限 |
|:--|:--|:--|
| `SYS_LOGS` | 系統運行錯誤、AOP 例外日誌 | 1 年 |
| `USER_LOGS` | 使用者行為稽核（查詢、下載、申請、審核） | 1 年 |
| `JOBS_CONF` | 背景任務配置（Cron 各分段、IS_ENABLED 開關） | 長期保留 |
| `JOBS_LOGS` | 所有排程 Job 執行歷史（含 Start/End Time、Status、Stack Trace） | 3 個月 |

---

## 3. SQL 執行策略

- **不使用預存程序 (Stored Procedures)**：所有複雜 SQL 邏輯集中於 Java 代碼中實作，確保業務邏輯集中管理且易於版本控制。
- **效能優化**：針對 Native SQL 常用的關聯欄位建立索引（Index）。
- **大批量讀寫**：使用 `JdbcPagingItemReader` 與 `JdbcBatchItemWriter` 提升效能。
- **簡單狀態更新**：使用共用的 JPA Repository（`Spring Data JPA`）。

---

## 4. 資料清理策略

| 資料類型 | 保留規則 |
|:--|:--|
| `MSG_DOWNLOAD` 合併實體檔案 | 合併後保留 **6 個月**（`EXPIRY_DATE` 欄位標記） |
| `SYS_LOGS` / `USER_LOGS` | 保留 **1 年**，由 `LogCleanupJob` 自動清理 |
| `JOBS_LOGS` | 保留 **3 個月**，由 `LogCleanupJob` 自動清理 |

---

## 5. 建置進度

- [ ] **Step 1**：初始化資料庫（建立 `MHH_DB`，匯入 DDL）
- [ ] **Step 2**：核心表單設計（`USER`、`MSG_HISTORY`、`MSG_APPROVAL`、`MSG_DOWNLOAD` 索引與關聯；`SYS_LOGS`、`USER_LOGS`、`JOBS_LOGS`）
- [ ] **Step 3**：排程控制表實施（`JOBS_CONF` 與 `JOBS_LOGS`）
- [ ] **Step 4**：JPA Repository 實作（Entity Mapping，配置外部來源 Sync 表）
- [ ] **Step 5**：資料清理策略驗證（測試 `LogCleanupJob` 正確清除過期資料）
