## 🤖 AI 快速上下文 (AI Context Block)

> 本段為給 AI 助理的結構化摘要，包含協助開發此專案所需的核心知識。

```yaml
project:
  name: MHH (Message History Hub)
  description: >
    金融機構內部的 SWIFT 電文歷史查詢平台。
    從 SWAL (Oracle) 與原始 PDF 檔案自動匯入電文，提供查詢、下載、稽核功能。
    系統設計為離線運行，不連接外部網路。
  language: Java 21
  framework: Spring Boot 4.0.5 + Spring Batch 5.x
  database: MS SQL Server 2019+
  frontend: Thymeleaf (layout) + Vue.js 3 (SPA logic) + Bootstrap 5

modules:
  mhh-common:
    role: 共用函式庫，不可單獨執行
    contains: Entity、JPA Repository、共用工具類
    note: AP 與 Batch 都強制依賴此模組，業務邏輯一致性由此保證
  mhh-core:
    role: 解析器框架（策略模式核心）
    contains: PdfParser 介面定義、電文類型識別邏輯
  mhh-ap:
    role: Web 應用，提供 RESTful API 與 Thymeleaf 視圖
    port: 8080
    key_controllers: [RouterController, DashboardController, MsgQueryController,
                      MsgDownloadController, UserCustomController, ParamController]
    auth: Spring Security + EIP SSO（開發模式可用 dev-mode 繞過）
    audit: Spring AOP 自動攔截，寫入 SYS_LOGS / USER_LOGS
  mhh-batch:
    role: 排程任務，不對外暴露 HTTP
    jobs: [SwallowSyncJob, PdfImportJob, HrSyncJob, ReservationMergeJob, LogCleanupJob]
    scheduling: 動態 Cron（從 DB JOBS_CONF 讀取，每 20 分鐘熱更新，無需重啟）
    data_access: 大批量用 JdbcPagingItemReader/JdbcBatchItemWriter，小操作用 JPA

key_tables:
  MSG_HISTORY:   SWIFT 電文歷史主表（MX + MT 共用）
  MSG_DOWNLOAD:  多筆預約下載申請與合併狀態
  MSG_APPROVAL:  Maker-Checker 審核工作流任務
  MSG_SWAL_SYNC: SWAL 原始資料暫存（同步前緩衝）
  USER:          使用者帳號（由 SSO 同步）
  USER_ROLE:     自訂角色權限映射（BRANCH_MAKER/CHECKER, PARAM_MAKER/CHECKER）
  USER_CUST:     使用者自定義屬性
  HR_USER:       員工在職狀態（HR 系統同步）
  JOBS_CONF:     排程 Job 配置（Cron 欄位、IS_ENABLED 開關）
  JOBS_LOGS:     Job 執行歷程（Status、Stack Trace），保留 3 個月
  SYS_LOGS:      系統例外日誌，保留 1 年
  USER_LOGS:     使用者行為稽核日誌，保留 1 年

key_design_decisions:
  - "不使用 Stored Procedure，所有 SQL 邏輯在 Java 層管理"
  - "PDF 解析採策略模式，新增電文類型只需新增 PdfParser 實作，不改核心邏輯"
  - "Maker-Checker 雙人複核：分行下載申請 & 參數設定維護都需主管放行"
  - "排程 Cron 由 JOBS_CONF 資料表控制，IT 改 DB 即生效，不需重新部署"
  - "所有稽核日誌只存 DB（不依賴檔案），符合金融資安規範"
  - "國別隔離：使用者只能存取對應國別的資料，跨國別需申請並經主管審核"

data_flows:
  swal_sync:    "SwallowSyncJob → Oracle SWAL (RO) → MSG_SWAL_SYNC → MSG_HISTORY"
  pdf_import:   "PdfImportJob → 掃描 \\data\\MX|MT → 識別類型 → 策略模式解析 → MSG_HISTORY + 移檔至 \\data\\ARCHIVE"
  bulk_download: "使用者申請 → MSG_DOWNLOAD (待放行) → 主管放行 → ReservationMergeJob 合併 PDF → \\data\\TEMP → 使用者下載"

dev_notes:
  dev_mode: "mhh.auth.dev-mode=true 可跳過 SSO，直接用 /dev/login 登入測試"
  local_db:  "使用 DB-init/docker-compose up -d 啟動本地 MS SQL Server 2022"
  build:     "mvn clean install -DskipTests（首次需下載依賴，耗時較長）"
  run_ap:    "mvn spring-boot:run -pl mhh-ap"
  run_batch: "mvn spring-boot:run -pl mhh-batch"

docs_index:
  overview:    docs/1_overview.md      # 架構圖 + 詳細資料流
  quickstart:  docs/2_quickstart.md    # 本地開發 & 生產部署
  user_guide:  docs/3_user_guide.md    # 操作說明（非技術用戶）
  design:      docs/4_design/README.md # 技術設計 + 建置進度總表
  operations:  docs/5_operations.md    # IT 運維、排程管理、日誌
```
