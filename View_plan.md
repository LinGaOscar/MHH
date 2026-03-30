# MHH View_plan (前台展示層建置計畫)

## 一、 技術架構 (Technical Architecture)
*   **前端框架**: Vue.js 3 (Composition API / Options API)
*   **模板引擎**: Thymeleaf (負責伺服器端渲染 Layout 與 HTML 初始分發)
*   **樣式與組件**: Bootstrap 5 + Vanilla CSS
*   **HTTP 客戶端**: Axios (整合 CSRF Token 與身分攔截)
*   **靜態資源**: 存放於 `src/main/resources/static/`。

## 二、 系統規劃 (System Planning)

### 2.1 頁面佈局 (Layout with Thymeleaf)
*   使用 Thymeleaf `layout:decorate` 功能。
*   **Shared Layout**: 包含 `AppHeader` (國別/語系/使用者資訊)、Sidebar 與 Footer。
*   **Router 導進**: `RouterController` 將請求導向對應的 Thymeleaf 頁面 (`dashboard.html`, `query.html` 等)。

### 2.2 核心視圖 (Views & Controllers Interface)
每個視圖進入後會自動初始化 Vue 實例。
*   **DashboardView**: 
    - 串接 `DashboardController`。
    - 顯示「代辦審核清單」與「預約下載進度」。
*   **MessageSearchView**: 
    - 串接 `MsgQueryController` (單筆預覽) 與 `MsgDownloadController` (預約提交)。
*   **ApprovalListView**: 
    - 統一由 `ApprovalService` 後端邏輯支撐各類 (分行/參數) 放行介面。
*   **ParamSettingsView**: 
    - 串接 `ParamController`。
    - 顯示角色功能對應清單，提供 MHH 內部權限提升之調整申請介面。

### 2.3 UI 組件 (Reusable Components)
*   `PdfPreviewModal`: 調用 `MsgQueryController` 提供異步 PDF 數據流。
*   `DownloadReservationModal`: 預約下載表單提交。
*   `CountrySwitcher`: 頂部國別切換組件。

## 三、 建置步驟 (Building Steps)
- [ ] **Step 1: Thymeleaf Layout 佈置**
    - 建立 `layout.html`, `fragments/header.html`。
- [ ] **Step 2: Vue 3 環境初始化**
    - 匯入離線 Vue.js 檔案，定義 Global Component。
- [ ] **Step 3: 頁面開發與 API 串接**
    - 實作 Dashboard 統計邏輯。
    - 實作電文查詢結果列表及預覽互動。
- [ ] **Step 4: 權限檢視頁開發**
    - 實作 `ParamSettingsView` 之腳本，確保國別權限隔離。
