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

### 2.3 常用指令
*   **查看初始化進度**:
    ```powershell
    docker logs -f mhh-db-init
    ```
*   **停止並移除容器**:
    ```powershell
    docker-compose down
    ```
*   **重置所有資料 (含磁碟卷)**:
    ```powershell
    docker-compose down -v
    ```

## 3. 注意事項 (Notes)
- **密碼強度**: MS SQL Server 預設密碼需符合複雜度要求（含大小寫英文字母、數字及特殊符號）。
- **磁碟佔用**: 資料庫實體檔案存放於 Docker Volume (`db-init_mssql-data`) 中。
- **離線開發**: 本環境完全於本地運行，不需連網。
