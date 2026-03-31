# MHH — Message History Hub

> 電文歷史平台：集中存儲、自動化解析、支援 MX/MT 多種 SWIFT 電文格式的金融後台系統。

---

## 📖 文件導覽

請依您的角色選擇對應文件：

| 文件 | 對象 | 說明 |
|:--|:--|:--|
| [系統全局概覽](docs/1_overview.md) | 所有人 | 系統目標、架構圖、資料流程，**建議第一篇閱讀** |
| [快速啟動指南](docs/2_quickstart.md) | 開發者 / IT | 本地開發環境建置與生產部署步驟 |
| [使用者操作手冊](docs/3_user_guide.md) | 一般使用者 | 系統登入、查詢、下載、審核流程說明 |
| [技術設計文件](docs/4_design/README.md) | 開發者 | 資料庫、AP、Batch、前端各模組設計規格與建置進度 |
| [IT 運維手冊](docs/5_operations.md) | IT / 維運人員 | 排程任務說明、日誌管理、部署路徑規劃、常見問題 |

---

## 🏗️ 技術棧速覽

| 層次 | 技術 |
|:--|:--|
| 語言 | Java 21 (OpenJDK / Temurin) |
| 框架 | Spring Boot 4.0.5 + Spring Batch 5.x |
| 前端 | Thymeleaf + Vue.js 3 + Bootstrap 5 |
| 資料庫 | MS SQL Server 2019+ |
| 建置工具 | Maven（多模組） |

---

## 📁 專案模組結構

```
MHH/
├── mhh-common/     # 共用 Entity、Repository、工具類
├── mhh-core/       # 核心業務邏輯（解析器框架）
├── mhh-ap/         # Web AP（RESTful API + Thymeleaf 視圖）
├── mhh-batch/      # 排程 Batch 任務（PDF 解析、資料同步）
├── DB-init/        # SQL Schema DDL 與測試資料
└── docs/           # 📄 所有說明文件
```
