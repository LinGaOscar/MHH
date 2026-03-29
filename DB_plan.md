# MHH DB_plan (資料庫規格建置計畫)

## 一、 技術架構 (Technical Architecture)
*   **資料庫引擎**: MS SQL Server 2019+
*   **字元集**: `UTF-8` (或資料庫對應之 N-charset)
*   **ORM 框架**: Hibernate 6.x (Spring Data JPA)
*   **外部設定**: `application.yml` (DataSource)

## 二、 系統規劃 (System Planning)
### 2.1 資料表清單 (Table List)
*   **使用者權限類 (User & Auth)**:
    - `USER`: 使用者基礎帳號。
    - `USER_ROLE`: 使用者自訂角色與權限映射。
*   **HR 同步類 (HR Sync)**:
    - `HR_USER`: 員工基本資料同步表。
    - `HR_UNIT`: 組織單位架構同步表。
*   **電文業務類 (Message Business - MSG_ 開頭)**:
    - `MSG_HISTORY`: SWIFT 電文歷史主表。
    - `MSG_DOWNLOAD`: 預約下載清單、合併狀態與下載連結紀錄。
    - `MSG_APPROVAL`: 電文下載、自訂角色申請之審核任務。
    - `MSG_SWAL_SYNC`: SWALLOW 原始資料同步至本系統之暫存表。
*   **系統監控類 (System & Monitoring)**:
    - `SYS_LOG`: 系統錯誤、啟動日誌與未預期異常。
    - `USER_LOG`: 稽核日誌，記錄查詢、下載預約、放行與駁回行為。
    - `JOBS_CONF`: 排程任務配置 (Cron, 參數, 開關)。
    - `JOBS_LOGS`: 排程任務執行歷史與失敗原因。

### 2.4 SQL 執行策略
*   **不使用預存程序 (Stored Procedures)**：所有複雜 SQL 邏輯集中於 `mhh-api` (Backend) 或 `mhh-worker` (Batch) 代碼中實作，確保商業邏輯集中管理且易於版本控制。
*   **效能優化**：針對 Native SQL 常用的關聯欄位建立索引 (Index)。

## 三、 建置步驟 (Building Steps)
- [ ] **Step 1: 初始化資料庫**
    - 建立 `MHH_DB`。
    - 匯入初始 DDL 指令碼。
- [ ] **Step 2: 核心表單設計**
    - 設計 `User`, `Message`, `ApprovalTask`, `DownloadReservation` 索引與關聯。
    - 建立 `SYS_LOG` 與 `USER_LOG` 表結構。
- [ ] **Step 3: 排程控制表實施**
    - 建立 `JOBS_CONF` 與 `JOBS_LOGS`。
- [ ] **Step 4: JPA Repository 實作**
    - 完成 Entity 與資料庫的 Mapping。
    - 配置讀取外部來源的 Sync 表。
