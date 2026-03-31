# 快速啟動指南 (Quickstart Guide)

本文件涵蓋兩個情境：
- **Part A**：DEV 環境建置（開發人員使用）
- **Part B**：PROD 環境部署規範（IT 人員使用）

---

## Part A：DEV 環境

### A.1 前置需求 (Prerequisites)

請確保開發機已安裝以下工具：

| 工具 | 版本需求 | 說明 |
|:--|:--|:--|
| Java | 21 (OpenJDK / Temurin) | 主要執行環境 |
| Maven | 3.9+ | 建置工具 |
| Docker Desktop | 最新穩定版 | 用於本地運行 MS SQL Server |

---

### A.2 路徑結構

DEV 環境採用 Maven 專案結構，根目錄為專案 Clone 位置（以下以 `D:\MHH` 為例）：

| 路徑 | 說明 |
|:--|:--|
| `\DB-init` | Docker Compose 及資料庫初始化腳本 |
| `\mhh-ap\src\main\resources\application.yml` | AP 模組配置文件（DEV 用） |
| `\mhh-batch\src\main\resources\application.yml` | Batch 模組配置文件（DEV 用） |
| `\mhh-ap\target` | AP 模組編譯輸出（`mhh-ap.jar`） |
| `\mhh-batch\target` | Batch 模組編譯輸出（`mhh-batch.jar`） |
| `\logs` | 系統日誌（`sys_logs.log`、`batch_logs.log`） |
| `\data\MX` | MX 電文來源目錄：存放待解析之 MX PDF 文件 |
| `\data\MT` | MT 電文來源目錄：存放待解析之 MT PDF 文件 |
| `\data\ARCHIVE` | 歸檔目錄：解析完成的 PDF 自動分類移至此處 |
| `\data\TEMP` | 暫存目錄：PDF 合併下載時的臨時緩衝區 |

---

### A.3 啟動資料庫

MHH 使用 MS SQL Server 2022，透過 Docker 一鍵啟動（含自動初始化 Schema 與測試資料）：

```powershell
# 進入 DB-init 目錄
cd DB-init

# 啟動容器（含自動初始化腳本）
docker-compose up -d

# 確認初始化完成
docker logs -f mhh-db-init
```

> 看到 `Database initialization completed.` 訊息即可繼續。

---

### A.4 編譯專案

在**根目錄**執行全模組編譯，首次執行會下載 Spring Boot 與 OpenPDF 依賴，請耐心等待：

```powershell
mvn clean install -DskipTests
```

---

### A.5 啟動各模組

#### mhh-ap（Web & API）

包含所有 RESTful 介面與 Thymeleaf 視圖。

```powershell
mvn spring-boot:run -pl mhh-ap
```

- **訪問位址**：`http://localhost:8080`
- **DEV 模式**：預設開啟 `mhh.auth.dev-mode: true`，可直接繞過 SSO 驗證進行測試。

#### mhh-batch（排程任務）

負責 PDF 解析匯入、SWAL 同步、PDF 合併與日誌清理等排程任務。

```powershell
mvn spring-boot:run -pl mhh-batch
```

- **驗證狀態**：啟動後模組會立即執行一次 `JOBS_CONF` 同步，隨後每 20 分鐘更新一次排程配置。

---

### A.6 常見問題排查

| 問題 | 解決方式 |
|:--|:--|
| **資料庫連線失敗** | 比對 `mhh-ap/src/main/resources/application.yml` 密碼是否與 `DB-init/docker-compose.yml` 一致 |
| **Maven 下載緩慢** | 第一次初始化依賴較多，屬正常現象，請耐心等待或設定近端 Maven Mirror |
| **Port 8080 被佔用** | 修改 `application.yml` 中的 `server.port` 設定 |

---

## Part B：PROD 環境

> 本段適用對象：IT 維運人員。  
> PROD 環境不應使用 Docker（安全考量），請直接部署 JAR 至 Windows Server。

### B.1 環境需求

| 項目 | 規格 |
|:--|:--|
| OS | Windows Server 2022+（或 Linux） |
| JRE | Java 21 Runtime (LTS) |
| Database | MS SQL Server 2019+ |
| 網路 | **離線運行**，不應連通網際網路 |

---

### B.2 路徑結構

PROD 環境僅部署打包好的檔案，建議根目錄為 `D:\MHH`：

| 路徑 | 說明 |
|:--|:--|
| `\bin` | 打包好的可執行 JAR 檔（`mhh-ap.jar`、`mhh-batch.jar`） |
| `\config` | 外部化 `application.yml` 配置文件（PROD 用） |
| `\logs` | 系統日誌（`sys_logs.log`、`batch_logs.log`） |
| `\data\MX` | MX 電文來源目錄：存放待解析之 MX PDF 文件 |
| `\data\MT` | MT 電文來源目錄：存放待解析之 MT PDF 文件 |
| `\data\ARCHIVE` | 歸檔目錄：解析完成的 PDF 自動分類移至此處 |
| `\data\TEMP` | 暫存目錄：PDF 合併下載時的臨時緩衝區 |

---

### B.3 打包與部署

**Step 1：在 DEV 環境打包 JAR**

```powershell
mvn clean package -DskipTests
# 將各模組 target\ 下的 JAR 複製至 PROD 主機的 \bin
```

**Step 2：啟動服務（指定外部配置）**

```powershell
# AP 模組
java -jar bin\mhh-ap.jar --spring.config.location=config\application.yml

# Batch 模組
java -jar bin\mhh-batch.jar --spring.config.location=config\application.yml
```

**Step 3：權限確認**

> 執行 Process 的帳戶必須對 `\data` 資料夾擁有「**讀取 / 寫入 / 刪除（移動）**」權限。
