# AP 模組設計規格 (AP Module Design)

← [返回設計文件導覽](README.md)

---

## 1. 技術架構

| 項目 | 規格 |
|:--|:--|
| 語言 | Java 21 (OpenJDK / Temurin) |
| 框架 | Spring Boot 4.0.5 + `spring-boot-starter-thymeleaf` |
| 安全控制 | Spring Security（SSO + Dev Mode 繞過） |
| 資料存取 | Spring Data JPA（基礎 CRUD）+ `NamedParameterJdbcTemplate`（複雜 Native SQL） |
| 視圖引擎 | Thymeleaf（Server-side Template，提供 Layout 與靜態資源引導） |
| 稽核日誌 | Spring AOP（自動化攔截並寫入 `SYS_LOGS` / `USER_LOGS`） |

---

## 2. Controller 清單

> 所有 Controller 均依據權限標籤進行 Access Control。

| Controller | 職責 |
|:--|:--|
| `RouterController` | `@Controller`，負責返回 Thymeleaf 模板路徑（`/index`、`/login`、`/dashboard` 等） |
| `UserLoginController` | 處理 SSO Token 擷取、`/dev/login` 繞過驗證邏輯與 `Logout` |
| `DashboardController` | 提供儀表板「待辦事項」統計與各業務狀態摘要 |
| `MsgQueryController` | 電文列表分頁查詢、單筆 PDF 內容預覽（Streaming） |
| `MsgDownloadController` | 單筆 PDF 直接下載（Logged）；多筆預約下載提交（`MSG_DOWNLOAD`）；進度查詢與最終檔案串流 |
| `UserCustomController` | `USER_CUST` 維護申請與查詢 |
| `ParamController` | 使用者/角色權限清單檢視；MHH 內部權限調高申請（維持國別隔離原則） |

---

## 3. Service 清單

| Service 介面 | 職責 |
|:--|:--|
| `ISwiftParserService` | MT/MX 電文結構化解析 |
| `IPdfEngineService` | PDF 繪製與多筆 PDF 合併（呼叫 `iText` 或 `PDFBox`） |
| `IMessageService` | 封裝電文查詢、歷史存取等業務邏輯 |
| `IApprovalService` | 通用 Maker-Checker 邏輯實作（含分行與參數業務隔離） |
| `IUserService` | SSO 使用者同步、HR 資訊整合、自訂角色權限管理 |
| `UserLogService` | 非同步寫入 `USER_LOGS`（`@Async` + `REQUIRES_NEW`，錯誤不影響主流程） |

---

## 4. Repository 清單

| Repository | 對應資料表 | 說明 |
|:--|:--|:--|
| `MsgIncomingRepository` | `MSG_INCOMING` | 進電 CRUD；含 `findByMessageId` |
| `MsgOutgoingRepository` | `MSG_OUTGOING` | 出電 CRUD；含 `findByMessageId` |
| `MsgDownloadRepository` | `MSG_DOWNLOAD` | 預約狀態管理 |
| `MsgApprovalRepository` | `MSG_APPROVAL` | 工作流任務管理 |
| `UserRepository` | `USER`、`USER_ROLE` | 使用者基礎操作 |
| `NativeQueryRepository` | 多表 | `NamedParameterJdbcTemplate` 高性能複雜 SQL |
| `UserLogRepository` | `USER_LOGS` | 使用者稽核日誌寫入；含 `deleteOlderThan` |

---

## 5. 建置進度

- [x] **Step 1**：專案初始化（Maven 多模組、Thymeleaf 配置）
- [x] **Step 2**：基礎架構開發（`UserActionAspect` AOP 日誌、`AuthEventListener` 登入稽核、Spring Security logout handler）
- [ ] **Step 3**：Service 層核心邏輯（PDF 合併與電文查詢）
- [ ] **Step 4**：Controller 接口逐一實作（`MsgQueryController`、`MsgDownloadController` 等）
- [ ] **Step 5**：整合測試（Maker-Checker 流程 + SSO 流程驗證）
