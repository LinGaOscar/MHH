# MHH AP_plan (應用程式介面建置計畫)

## 一、 技術架構 (Technical Architecture)
*   **語言版本**: Java 21 (OpenJDK/Temurin)
*   **框架版本**: Spring Boot 4.0.5
*   **核心組件**: 
    - **Spring Security**: 處理 SSO 與開發模式驗證。
    - **Spring Data JPA**: 資料持久化 (基礎 CRUD)。
    - **NamedParameterJdbcTemplate**: 處理複雜、高效能 Native SQL 查詢。
    - **Spring AOP**: 實作自動化稽核日誌，系統例外記錄至 `SYS_LOGS`。
    - **Spring Validation**: 入參檢核與權限標籤校驗。

## 二、 系統規劃 (System Planning)
### 2.1 專案結構
*   採用 Maven 多模組結構：
    - `mhh-api`: 提供 RESTful 端點。
    - `mhh-common`: 共享 Entity (`USER`, `MSG_HISTORY` 等)、Dto 與工具類。
    - `mhh-core`: 主要業務邏輯與 Service 層。

### 2.2 身分驗證邏輯
*   **SSO 模式**: 透過 `SsoVerificationFilter` 解析解密 Token，並與 `HR_USER` / `HR_UNIT` 同步資料。
*   **開發模式 (Dev Mode)**: 當 `mhh.auth.dev-mode: true` 時，啟用 `DevLoginController` 接受 `/dev/login` 請求。

### 2.3 核心服務設計
*   `ISwiftParserService`: 電文 (MT/MX) 結構化解析。
*   `IPdfEngineService`: 電文轉 PDF 及合併 PDF。
*   `LoggingService`: AOP 實作雙重寫入 (`SYS_LOGS`：系統錯誤與例外, `USER_LOGS`：使用者行為)。
*   **資料存取策略**: 
    - 簡單查詢: 使用 JPA Repository。
    - 複雜/高效能查詢: 於 `NativeQueryRepository` 中實作 SQL。

## 三、 建置步驟 (Building Steps)
- [ ] **Step 1: 專案初始化**
    - 建立 Maven 多模組結構。
    - 配置 `application.yml` (DataSource, FTP, SSO Key)。
- [ ] **Step 2: 基礎服務開發**
    - 實作 AOP Audit Aspect。
    - 建立全局異常處理器 (`GlobalExceptionHandler`)。
- [ ] **Step 3: 安全性與驗證實作**
    - 整合 Spring Security。
    - 實作 `SsoVerificationFilter` 與 `DevLoginController`。
- [ ] **Step 4: 業務 API 開發**
    - `MessageApiController`: 查詢、PDF 預覽、單筆串流下載。
    - `DownloadReservationController`: 提交 `MSG_DOWNLOAD` 預約、查詢進度與合併後檔案下載。
    - `UserCustController`: `USER_CUST` 表格之 CRUD 申請 (由 `PARAM_MAKER` 發起)。
    - `ApprovalApiController`: 針對 `USER_ROLE`、`MSG_APPROVAL` 與 `USER_CUST` 之放行審核 (需實現「分行類」與「參數類」之業務隔離)。
