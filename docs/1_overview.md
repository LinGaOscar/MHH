# 系統全局概覽 (System Overview)

> 建議閱讀順序：本文件 → [快速啟動](2_quickstart.md) → [技術設計](4_design/README.md)

---

## 1. 核心目標

| 目標 | 說明 |
|:--|:--|
| **集中存儲** | 將來自不同管道（SWAL 資料庫、原始 PDF）的電文整合至 MHH 資料庫 |
| **自動化處理** | 透過排程 Batch Jobs 減少人工干預，支援動態 Cron 配置 |
| **多樣化支援** | 策略模式設計，支援 ISO 20022 MX 與 SWIFT MT 多種電文類型解析 |
| **稽核合規** | 符合金融稽核標準，完整記錄使用者行為（查詢、下載、審核、權限變更） |

---

## 2. 系統架構

```
┌─────────────────────────────────────────────────────────────┐
│                      外部資料來源                              │
│   ┌───────────────┐          ┌────────────────────────────┐  │
│   │  SWAL 資料庫   │          │  PDF 電文目錄 (MX/ MT/)     │  │
│   │   (Oracle)    │          │  - MX: pacs.008, camt.054  │  │
│   └───────┬───────┘          │  - MT: MT103, MT202        │  │
│           │                  └────────────┬───────────────┘  │
└───────────┼───────────────────────────────┼──────────────────┘
            │                               │
            ▼                               ▼
┌───────────────────────────────────────────────────────────────┐
│                   mhh-batch（排程任務）                         │
│   SwallowSyncJob           PdfImportJob                       │
│   HrSyncJob                LogCleanupJob                      │
│   ReservationMergeJob      (動態 Cron，由 JOBS_CONF 控制)       │
└───────────────────────────┬───────────────────────────────────┘
                            │ 寫入
                            ▼
┌───────────────────────────────────────────────────────────────┐
│                   MHH 資料庫 (MS SQL Server)                   │
│  MSG_INCOMING  MSG_OUTGOING   MSG_DOWNLOAD   MSG_APPROVAL     │
│  MSG_SWAL_SYNC                                                 │
│  USER   USER_ROLE   USER_CUST   HR_USER   HR_UNIT             │
│  SYS_LOGS   USER_LOGS   JOBS_LOGS   JOBS_CONF                 │
└───────────────────────────┬───────────────────────────────────┘
                            │ 讀取
                            ▼
┌───────────────────────────────────────────────────────────────┐
│                   mhh-ap（Web 應用）                            │
│   Spring Security (SSO)    Thymeleaf 視圖                      │
│   RESTful API              AOP 稽核日誌                        │
│   Maker-Checker 審核流程                                        │
└───────────────────────────┬───────────────────────────────────┘
                            │
                            ▼
                     ┌──────────────┐
                     │  使用者瀏覽器  │
                     │ Vue.js 3 SPA │
                     └──────────────┘
                     (Single Page Application - 單頁式應用)
```

---

## 3. 資料流程詳述

### 流程 A：SWAL 資料庫同步

**目標**：從外部 SWAL 系統同步已格式化的電文數據。

1. **觸發**：定時批次任務（Cron 由 `JOBS_CONF` 動態配置）
2. **來源**：外部 SWAL 資料庫（Oracle，**唯讀**連線）
3. **處理**：
   - 抓取自上次同步時間點後的新紀錄
   - 數據對應並轉換至 MHH Schema
4. **目的地**：`MSG_SWAL_SYNC`（暫存）→ `MSG_INCOMING`（進電主表）

### 流程 B：PDF 電文匯入與解析

**目標**：將 MX/MT 資料夾中的原始 PDF 文件解析為結構化數據。

1. **觸發**：`PdfImportJob` 定時掃描目錄
2. **來源**：`\data\MX\`（ISO 20022 格式）、`\data\MT\`（SWIFT MT 格式）
3. **處理邏輯（策略模式）**：
   - 掃描目錄偵測新檔案
   - 依文件內文識別電文類型（如：MT103, pacs.008）
   - 動態選擇對應的 `PdfParser` 實作
   - 提取業務欄位（發報行、收報行、金額、幣別、交易編號等）
4. **目的地**：進電寫入 `MSG_INCOMING`，出電寫入 `MSG_OUTGOING`；原始 PDF 移至 `\data\ARCHIVE\`

### 流程 C：PDF 合併下載（使用者觸發）

1. 使用者選擇多筆電文，送出「預約下載」申請 → 寫入 `MSG_DOWNLOAD`
2. 主管於 Dashboard 審核放行 → 狀態更新為「已放行」
3. `ReservationMergeJob` 自動執行 PDF 合併 → 暫存至 `\data\TEMP\`
4. 使用者可於 Dashboard 下載最終合併檔，`USER_LOGS` 記錄此次操作

---

## 4. 多電文類型處理策略

為應對多種電文種類，系統採用以下設計：

- **策略模式 (Strategy Pattern)**：每種電文類型有其專屬的 `PdfParser` 實作
- **彈性對應**：使用元數據驅動或特定 DTO 應對不同格式
- **可擴展性**：新增 MX/MT 類型只需新增一個 Parser 類別，**無需修改核心批次邏輯**

---

## 5. 模組與技術棧

| 模組 | 職責 | 核心技術 |
|:--|:--|:--|
| `mhh-common` | 共用 Entity、Repository、工具類 | Spring Data JPA、Hibernate 6.x |
| `mhh-core` | 解析器框架與策略介面定義 | Java Interface / Strategy Pattern |
| `mhh-ap` | Web 應用：API + 視圖 + 安全控制 | Spring MVC、Spring Security、Thymeleaf、Vue.js 3 |
| `mhh-batch` | 排程任務：同步、解析、清理 | Spring Batch 5.x、TaskScheduler |
| `DB-init` | 資料庫初始化腳本 | MS SQL Server DDL |
