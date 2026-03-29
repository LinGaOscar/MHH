# MHH (電文歷史平台) README

本文件提供 MHH 系統的 **使用者操作說明** 與 **IT 運維說明**。

---

## 一、 使用者操作說明 (User Manual)

### 1.1 系統登入
*   本系統透過 **EIP SSO** 進行身分驗證，您不需要額外輸入密碼。
*   登入後，系統會檢查您的 **SWALLOW 權限** 與 **HR 在職狀態**。
*   若您擁有跨國別權限，請由右上角選單切換（切換時將重置登入狀態並變更語系）。

### 1.2 Dashboard 儀表板
*   **代辦事項**: 顯示您目前身分需要處理的電文或申請任務。
*   **待放行提示**: 若您是主管角色，此區域會顯示待您審核的申請案（自訂角色或批次下載）。

### 1.3 電文查詢與下載
*   **查詢**: 進入查詢頁面，輸入 Sender/Receiver BIC 或日期區間。
*   **預約下載**: 
    - 若選擇單筆，可直接點選「PDF 預覽」後直接下載。
    - 若選擇多筆，系統會彈出「預約下載」申請視窗，確認後建立審核任務。
    - 經主管於 Dashboard 放行後，背景任務會自動執行 PDF 合併作業。
    - 合併完成後，您可於 Dashboard 的「下載進度」區域點選下載最終合併檔。

### 1.4 審核流程 (Maker-Checker)
*   **經辦**: 發起申請後，狀態會更新為「待放行」。
*   **主管**: 在待辦事項中點選審核，確認無誤後點選「放行」。

---

## 二、 IT 運維說明 (IT Operations Manual)

### 2.1 環境需求
*   **OS**: Windows Server 2022+ (or Linux)
*   **JRE**: Java 21 Runtime (LTS)
*   **Database**: MS SQL Server 2019+
*   **Framework**: Spring Boot 4.0.5
*   **網路**: 本系統設計為 **離線運行**，不應連通網際網路。

### 2.2 部署指引
1.  **靜態資源**: 確保 `src/main/resources/static/lib` 中包含所有必要的 Vue.js, Bootstrap 等本地腳本。
2.  **App 部署**: 
    - `mhh-api.jar`: 執行為獨立之 Java Process 或註冊為 Windows Service。
    - `mhh-worker.jar`: 執行為獨立之 Java Process (若與 API 分開部署)。
3.  **組態設定**: 於 `application.yml` 中配置 Spring DataSource、FTP 位址以及 SSO 驗證之解密金鑰。
4.  **開發模式 (Dev Mode)**: 
    *   若需繞過 SSO 進行測試，請於 `application.yml` 設定 `mhh.auth.dev-mode: true`。
    *   啟用後可存取 `/dev/login` 進行簡易帳密登入。
    *   **[警告] 於生產環境部署時，此項配置必須設定為 false 或移除。**

### 2.3 每日任務排程 (Spring Batch)
*   **SWAL Sync (SwallowSyncJob)**: 每日凌晨 01:00 執行。
*   **HR Sync (HrSyncJob)**: 每日凌晨 02:00 執行。
*   **PDF Import (PdfImportJob)**: 每日 23:00 執行。

### 2.4 日誌查詢 (SYS_LOG & USER_LOG)
*   **SYS_LOG**: 用於排除系統錯誤與排程失敗原因。
*   **USER_LOG**: 用於符合金管需求之操作稽核，記錄所有查詢、下載與審核行為。
