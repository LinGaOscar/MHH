# 技術設計文件導覽 (Design Documents Index)

本目錄為 MHH 各模組的詳細技術設計規格，適用對象：**開發人員**。

> 建議閱讀順序：`db.md` → `ap.md` → `batch.md` → `view.md`

---

## 文件清單

| 文件 | 說明 |
|:--|:--|
| [db.md](db.md) | 資料庫規格：資料表設計、SQL 策略、清理規則 |
| [ap.md](ap.md) | AP 模組：Controller、Service、Repository 清單與職責 |
| [batch.md](batch.md) | Batch 模組：Job 設計、動態排程機制、執行流程 |
| [view.md](view.md) | 前端視圖：頁面架構、Vue 組件、Thymeleaf Layout |

---

## 全局建置進度追蹤

### 🗄️ DB 模組

- [ ] **Step 1**：初始化資料庫（建立 `MHH_DB`，匯入 DDL）
- [ ] **Step 2**：核心表單設計（`USER`、`MSG_HISTORY`、`MSG_APPROVAL`、`MSG_DOWNLOAD` 索引與關聯）
- [ ] **Step 3**：排程控制表實施（`JOBS_CONF`、`JOBS_LOGS`）
- [ ] **Step 4**：JPA Repository 實作（Entity Mapping、Sync 表配置）
- [ ] **Step 5**：資料清理策略實作

### 🖥️ AP 模組

- [x] **Step 1**：專案初始化（Maven 多模組、Thymeleaf 配置）
- [ ] **Step 2**：基礎架構開發（AOP 日誌、SSO 安全過濾器）
- [ ] **Step 3**：Service 層核心邏輯（PDF 合併與電文解析）
- [ ] **Step 4**：Controller 接口實作
- [ ] **Step 5**：整合測試（Maker-Checker 與 SSO 流程）

### ⚙️ Batch 模組

- [x] **Step 1**：專案初始化與模組結構（依賴 `mhh-common`）
- [ ] **Step 2**：動態排程框架（`DynamicJobService`、`JobRefresher`）
- [ ] **Step 3**：`SwallowSyncJob` 實作（SWAL → `MSG_SWAL_SYNC`）
- [ ] **Step 4**：`PdfImportJob` + 策略模式 Parser 實作
- [ ] **Step 5**：`LogCleanupJob` + `ReservationMergeJob` 實作

### 🎨 View 模組

- [ ] **Step 1**：Thymeleaf Layout 建置（`layout.html`、`fragments/`）
- [ ] **Step 2**：Vue 3 環境初始化（離線引入、Global Component 定義）
- [ ] **Step 3**：Dashboard 與電文查詢頁開發
- [ ] **Step 4**：權限檢視頁開發（國別隔離）
