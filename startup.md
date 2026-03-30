# MHH 系統啟動指南 (Startup Guide)

本文件說明如何從零開始啟動 MHH 系統的資料庫與各個應用模組。

## 1. 前置需求 (Prerequisites)

在開始之前，請確保您的開發環境已安裝以下工具：
*   **Java 21** (OpenJDK/Temurin)
*   **Maven 3.9+**
*   **Docker Desktop** (運行 MS SQL Server)

## 2. 啟動資料庫 (Database Setup)

MHH 使用 MS SQL Server 2022。請透過 Docker 一鍵啟動：

1.  進入 `DB-init` 目錄：
    ```powershell
    cd DB-init
    ```
2.  啟動容器 (包含自動初始化腳本)：
    ```powershell
    docker-compose up -d
    ```
3.  確認初始化狀態：
    ```powershell
    docker logs -f mhh-db-init
    ```
    *看到 `Database initialization completed.` 即可繼續。*

## 3. 編案與安裝 (Build Project)

在根目錄執行全模組編譯，下載必要的依賴庫：

```powershell
mvn clean install -DskipTests
```

## 4. 啟動應用模組 (Running Modules)

### 4.1 啟動 mhh-ap (Web & API)
此模組包含所有的 RESTful 介面與 Thymeleaf 視圖。

*   **透過 Maven 啟動**:
    ```powershell
    mvn spring-boot:run -pl mhh-ap
    ```
*   **訪問位址**: `http://localhost:8080`
*   **開發模式**: 預設開啟 `mhh.auth.dev-mode: true`，您可以直接訪問並測試。

### 4.2 啟動 mhh-batch (Background Jobs)
此模組負責執行電文同步、PDF 合併與日誌清理等排程任務。

*   **透過 Maven 啟動**:
    ```powershell
    mvn spring-boot:run -pl mhh-batch
    ```
*   **驗證狀態**: 啟動後，模組會每 20 分鐘與資料庫 `JOBS_CONF` 表同步排程配置（IT 人員可直接在 DB 中修改 Cron 與開關狀態）。

---

## 5. 常見問題 (Troubleshooting)

- **資料庫連線失敗**: 檢查 `mhh-ap/src/main/resources/application.yml` 中的密碼是否與 `DB-init/docker-compose.yml` 一致。
- **Maven 下載緩慢**: 由於是第一次初始化，下載 Spring Boot 與 OpenPDF 依賴可能耗時較久，請耐心等待。
