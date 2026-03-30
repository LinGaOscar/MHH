# MHH AP_plan (應用程式介面建置計畫)

## 一、 技術架構 (Technical Architecture)
*   **語言版本**: Java 21 (OpenJDK/Temurin)
*   **框架版本**: Spring Boot 4.0.5 (引入 `spring-boot-starter-thymeleaf`)
*   **核心組件**: 
    - **Spring Security**: 處理 SSO 與開發模式驗證。
    - **Spring Data JPA**: 資料持久化 (基礎 CRUD)。
    - **NamedParameterJdbcTemplate**: 處理複雜、高效能 Native SQL 查詢。
    - **Thymeleaf**: 作為 Server-side Template Engine 提供前端 Layout 與靜態資源引導。
    - **Spring AOP**: 實作自動化稽核日誌。

## 二、 系統模組清單 (System Modules)

### 2.1 Controller 清單 (RESTful & View Routing)
所有 Controller 均需依據權限權限標籤進行 Access Control。
*   **RouterController**: 
    - 使用 Spring MVC `@Controller`。
    - 負責返回 Thymeleaf 模板路徑 (如 `/index`, `/login`, `/dashboard`)。
*   **DashboardController**: 
    - 提供儀表板「待辦事項」統計與各業務狀態摘要。
*   **UserLoginController**: 
    - 處理 SSO Token 擷取、`/dev/login` 繞過驗證邏輯與 `Logout`。
*   **MsgQueryController**: 
    - 提供電文列表分頁查詢、單筆 PDF 內容預覽 (Streaming)。
*   **MsgDownloadController**: 
    - 單筆 PDF 直接下載 (Logged)。
    - 多筆預約下載提交 (`MSG_DOWNLOAD` 申請)。
    - 預約合併進度查詢與最終檔案串流。
*   **UserCustomController**: 
    - 處理 `USER_CUST` 之維護申請與查詢。
*   **ParamController**: 
    - 提供目前使用者/角色權限清單檢視。
    - 處理 MHH 系統內部的權限調高申請 (維持國別隔離原則)。

### 2.2 Service 清單 (Business Logic)
*   `ISwiftParserService`: MT/MX 電文結構化解析。
*   `IPdfEngineService`: PDF 繪製與多筆 PDF 合併 (呼叫 `iText` 或 `PDFBox`)。
*   `IMessageService`: 封裝電文查詢、歷史存取等邏輯。
*   `IApprovalService`: 實作通用 Maker-Checker 邏輯 (含分行與參數業務隔離)。
*   `IUserService`: 處理 SSO 使用者同步、HR 資訊與自訂角色權限。
*   `ILoggingService`: AOP 實作之雙重日誌記錄 (`SYS_LOGS`, `USER_LOGS`)。

### 2.3 Repository 清單 (Data Access)
*   `MsgHistoryRepository`: `MSG_HISTORY` 基礎 JPA 操作。
*   `MsgDownloadRepository`: `MSG_DOWNLOAD` 預約狀態管理。
*   `MsgApprovalRepository`: `MSG_APPROVAL` 任務管理。
*   `UserRepository`: `USER`, `USER_ROLE` 基礎操作。
*   `NativeQueryRepository`: 使用 `NamedParameterJdbcTemplate` 實作之高性能 SQL。
*   `SysLogRepository` / `UserLogRepository`: 稽核日誌寫入。

## 三、 建置步驟 (Building Steps)
- [x] **Step 1: 專案初始化** (Maven 多模組、Thymeleaf 配置)
- [ ] **Step 2: 基礎架構開發** (AOP 日誌、SSO 安全過濾器)
- [ ] **Step 3: Service 層核心邏輯** (PDF 合併與電文解析)
- [ ] **Step 4: Controller 接口實作** (依上述清單逐一實作)
- [ ] **Step 5: 整合測試** (驗證 Maker-Checker 與 SSO 流程)
