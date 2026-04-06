# MHH 資料庫初始化工具 (DB-init)

本目錄提供 MHH 系統開發所需之資料庫環境自動化建置方案。

## 1. 啟用步驟 (Activation)

請確認環境已安裝並啟動 **Docker Desktop**。

1.  開啟終端機 (PowerShell 或 CMD)。
2.  進入 `DB-init` 目錄：
    ```powershell
    cd DB-init
    ```
3.  啟動容器並執行初始化腳本：
    ```powershell
    docker-compose up -d
    ```
    *   **註**: 啟動後 `mhh-db-init` 容器將自動執行 DDL 與測試資料匯入。

## 2. 使用步驟 (Usage)

### 2.1 連線方式 (Connection Methods)

#### A. 圖形化介面 (GUI Tools)
支援 **SSMS (SQL Server Management Studio)** 或 **Azure Data Studio** 連線：
*   **Server Name**: `localhost,1433`
*   **Authentication**: `SQL Server Authentication`
*   **Login**: `sa`
*   **Password**: `YourPassword123`
*   **Default Database**: `MHH_DB`

#### B. 命令列介面 (CLI)
使用 Docker 容器內之 `sqlcmd` 工具：
```powershell
# 進入互動式介面
docker exec -it mhh-sqlserver /opt/mssql-tools2/bin/sqlcmd -S localhost -U sa -P YourPassword123 -d MHH_DB

# 執行單次查詢
docker exec -it mhh-sqlserver /opt/mssql-tools2/bin/sqlcmd -S localhost -U sa -P YourPassword123 -d MHH_DB -Q "SELECT * FROM [USER]"
```

#### C. 應用程式連線 (Spring Boot / JDBC)
```yaml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=MHH_DB;encrypt=true;trustServerCertificate=true
    username: sa
    password: YourPassword123
```

### 2.2 驗證建置結果
可透過 SSMS 或 Azure Data Studio 執行下列查詢驗證：
```sql
USE MHH_DB;
SELECT * FROM [USER]; -- 預期包含 ADMIN 與測試帳號
SELECT * FROM [JOBS_CONF]; -- 預期包含初始排程配置
```

### 2.3 排程任務維護
由於 `mhh-batch` 採動態排程機制，IT 人員可直接於 `JOBS_CONF` 資料表調整任務設定。變動將於 **20 分鐘內** 自動生效。

*   **查詢任務狀態**:
    ```sql
    SELECT * FROM [JOBS_CONF];
    ```
*   **資料表說明**:
    *   `JOBS_CONF`: 任務主表，定義 Cron 設定與啟用狀態。
    *   `USER_LOGS`: 稽核日誌。保留期限為 **1 年**，由 `LogCleanupJob` 定期清理。

*   **日誌路徑**（`d:/MHH_FILES/LOGS/`）:
    *   `batch.log`：批次作業記錄，保留 **90 天**（Logback 自動滾動）。
    *   `sys.log`：系統異常 (WARN/ERROR) 記錄，保留 **1 年**（Logback 自動滾動）。

*   **暫停特定任務 (如：暫停同步)**:
    ```sql
    UPDATE [JOBS_CONF] SET [IS_ENABLED] = 0 WHERE [JOB_NAME] = 'SwallowSyncJob';
    ```
*   **修改執行週期 (如：改為每小時整點觸發)**:
    ```sql
    UPDATE [JOBS_CONF] SET [CRON_MIN] = '0' WHERE [JOB_NAME] = 'ReservationMergeJob';
    ```

> [!NOTE]
> **Cron 標準說明**: 
> 採 Spring Cron 標準，其中 `CRON_DOW` (週) 之 `1` 代表週日 (SUN)，`7` 代表週六 (SAT)。

## 3. 注意事項 (Notes)
- **密碼強度**: 需符合 MS SQL Server 預設複雜度要求。
- **資料儲存**: 資料庫檔案存放於 Docker Volume (`db-init_mssql-data`)。
- **環境獨立**: 本環境完全於本地運行，無需網際網路連線。
