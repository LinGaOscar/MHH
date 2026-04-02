# MHH 資料庫初始化工具 (DB-init)

本目錄提供 MHH 系統開發所需的資料庫環境自動化建置方案。

## 1. 啟用步驟 (Activation)

請確保您的電腦已安裝 **Docker Desktop** 並確保其正在運行。

1.  開啟終端機 (PowerShell 或 CMD)。
2.  進入 `DB-init` 目錄：
    ```powershell
    cd DB-init
    ```
3.  一鍵啟動容器與初始化腳本：
    ```powershell
    docker-compose up -d
    ```
    *   **註**: 啟動後，`mhh-db-init` 容器會自動執行 DDL 與測試資料匯入。

## 2. 使用步驟 (Usage)

### 2.1 連線方式 (Connection Methods)

#### A. 圖形化工具 (GUI Tools)
可以使用 **SSMS (SQL Server Management Studio)** 或 **Azure Data Studio** 連線：
*   **Server Name**: `localhost,1433`
*   **Authentication**: `SQL Server Authentication`
*   **Login**: `sa`
*   **Password**: `YourPassword123`
*   **Default Database**: `MHH_DB`

#### B. 執行列 / 終端機 (CLI)
直接使用 Docker 容器內的 `sqlcmd` 工具：
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
您可以使用 SQL Server Management Studio (SSMS) 或 Azure Data Studio 連線後執行：
```sql
USE MHH_DB;
SELECT * FROM [USER]; -- 應該會看到 ADMIN 與測試帳號
SELECT * FROM [JOBS_CONF]; -- 應該會看到初始排程配置
```

### 2.3 排程任務維護 (IT 用)
由於 `mhh-batch` 採動態排程機制且無前端介面，IT 人員需直接在 `JOBS_CONF` 表中操作以調整任務。所有變動會在 **20 分鐘內** 自動生效。

*   **查看所有任務狀態**:
    ```sql
    SELECT * FROM [JOBS_CONF];
    ```
*   **資料表說明**:
    *   `JOBS_CONF`: 任務主表，包含各項 Job 的 Cron 設定與開關。
    *   `USER_LOGS`: 使用者行為稽核日誌（登入/查詢/預約/下載，保留 **1 年**，由 `LogCleanupJob` 定期清理）。

*   **本機日誌檔案**（`d:/MHH_FILES/LOGS/`）:
    *   `batch.log` / `batch.YYYY-MM-DD.log`：批次作業執行記錄，保留 **90 天**（Logback 自動滾動）。
    *   `sys.log` / `sys.YYYY-MM-DD.log`：系統 WARN / ERROR 異常，保留 **1 年**（Logback 自動滾動）。
*   **暫停某個任務 (例如：暫停同步)**:
    ```sql
    UPDATE [JOBS_CONF] SET [IS_ENABLED] = 0 WHERE [JOB_NAME] = 'SwallowSyncJob';
    ```
*   **修改執行時間 (例如：改為每小時合併一次)**:
    ```sql
    -- 將分鐘設為 0，其餘保持原樣即可實現每小時整點觸發
    UPDATE [JOBS_CONF] SET [CRON_MIN] = '0' WHERE [JOB_NAME] = 'ReservationMergeJob';
    ```

> [!NOTE]
> **Cron 標準說明**: 
> 系統採用 Spring Cron 標準，其中 `CRON_DOW` (週) 的 `1` 代表週日 (SUN)，`7` 代表週六 (SAT)。

### 2.4 系統常用指令

## 3. 注意事項 (Notes)
- **密碼強度**: MS SQL Server 預設密碼需符合複雜度要求（含大小寫英文字母、數字及特殊符號）。
- **磁碟佔用**: 資料庫實體檔案存放於 Docker Volume (`db-init_mssql-data`) 中。
- **離線開發**: 本環境完全於本地運行，不需連網。
