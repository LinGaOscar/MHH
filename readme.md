# MHH — Message History Hub

> 電文歷史平台：集中存儲、自動化解析、支援 MX/MT 多種 SWIFT 電文格式的金融後台系統。

---

## 📖 文件導覽

| 編號 | 文件名稱 | 對象 | 說明 |
|----------|----------|----------|----------|
| 1 | [系統概覽](docs/1.overview.md) | 全體 | 系統架構圖與數據流導覽，**推薦優先閱讀** |
| 2 | [快速啟動指南 (DEV)](docs/2.quickstart.md) | 開發人員 | 開發環境建置與本地啟動步驟 |
| 3 | [使用者操作手冊](docs/3.user_guide.md) | 使用人員 | 登入、查詢、下載與雙人審核流程 |
| 4.1 | [技術設計導覽](docs/4.design_guide.md) | 開發人員 | AP、Batch、DB 與前端設計細節 |
| 7.1 | [電文規格導覽](docs/7.msg_guide.md) | 業務與測試人員 | MT/MX 電文欄位對照與 SWAL 映射關係 |
| 9 | [生產維運手冊](docs/9.prod_operations.md) | 維運人員 | **PROD 專屬**：部署、監控與故障排除 |

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
