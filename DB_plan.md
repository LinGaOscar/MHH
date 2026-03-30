# MHH DB_plan (資料庫規格建置計畫)

## 一、 技術架構 (Technical Architecture)
*   **資料庫引擎**: MS SQL Server 2019+
*   **字元集**: `UTF-8` (或資料庫對應之 N-charset)
*   **ORM 框架**: Hibernate 6.x (Spring Data JPA)
*   **外部設定**: `application.yml` (DataSource)

## 二、 系統規劃 (System Planning)
### 2.1 資料表清單 (Table List)
*   **使用者權限類 (User & Auth)**:
    - `USER`: 使用者基礎帳號表。
    - `USER_ROLE`: 使用者自訂角色與權限映射表 (含 `BRANCH_MAKER`, `BRANCH_CHECKER`, `PARAM_MAKER`, `PARAM_CHECKER`)。
    - `USER_CUST`: 自定義使用者屬性配置表格。
*   **HR 同步類 (HR Sync)**:
    - `HR_USER`: 員工基本資料同步表。
    - `HR_UNIT`: 組織單位架構同步表。
*   **電文業務類 (Message Business - MSG_ 開頭)**:
    - `MSG_HISTORY`: SWIFT 電文歷史主表。
    - `MSG_DOWNLOAD`: 預約下載詳情與合併狀態表 (含 `EXPIRY_DATE`, `FILE_SIZE`)。單筆下載免放行，多筆需預約。
    - `MSG_APPROVAL`: 工作流審核任務表。
    - `MSG_SWAL_SYNC`: SWALLOW 原始資料同步至本系統之暫存表。
*   **系統監控類 (System & Monitoring)**:
    - `SYS_LOGS`: 系統運行錯誤、AOP 例外日誌 (保留一年)。
    - `USER_LOGS`: 使用者行為稽核日誌 (查詢、下載、申請、審核，保留一年)。
    - `JOBS_CONF`: 背景任務配置 (Cron, 參數, 開關)。
    - `JOBS_LOGS`: 所有排程 Job 執行歷史 (含 Start/End Time, Status, Stack Trace)。

### 2.4 SQL 執行策略
*   **不使用預存程序 (Stored Procedures)**：所有複雜 SQL 邏輯集中於 `mhh-api` (Backend) 或 `mhh-worker` (Batch) 代碼中實作，確保商業邏輯集中管理且易於版本控制。
*   **效能優化**：針對 Native SQL 常用的關聯欄位建立索引 (Index)。

## 三、 建置步驟 (Building Steps)
- [ ] **Step 1: 初始化資料庫**
    - 建立 `MHH_DB`。
    - 匯入初始 DDL 指令碼。
- [ ] **Step 2: 核心表單設計**
    - 設計 `USER`, `USER_ROLE`, `USER_CUST`, `MSG_HISTORY`, `MSG_APPROVAL`, `MSG_DOWNLOAD` 索引與關聯。
    - 建立 `SYS_LOGS`, `USER_LOGS`, `JOBS_LOGS` 表結構。
- [ ] **Step 3: 排程控制表實施**
    - 建立 `JOBS_CONF` 與 `JOBS_LOGS`。
- [ ] **Step 4: JPA Repository 實作**
    - 完成 Entity 與資料庫的 Mapping。
    - 配置讀取外部來源的 Sync 表。
- [ ] **Step 5: 資料清理策略**
    - `MSG_DOWNLOAD` 已合併之實體檔案保留 6 個月。
    - `SYS_LOGS` 與 `USER_LOGS` 保留 1 年。
