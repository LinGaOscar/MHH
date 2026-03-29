# MHH View_plan (前台展示層建置計畫)

## 一、 技術架構 (Technical Architecture)
*   **前端框架**: Vue.js 3 (Composition API)
*   **樣式與組件**: Bootstrap 5 + Vanilla CSS
*   **靜態資源**: 離線存放於 `src/main/resources/static/lib`。
*   **HTTP 客戶端**: Axios (含全局攔截器)。
*   **i18n**: Vue-i18n (處理多語系需求)。

## 二、 系統規劃 (System Planning)
### 2.1 核心介面串接
*   **API 整合**: 所有前端請求必須透過 Axios 與 `mhh-api` (後端) 進行串接。
*   **預約流程**: 多選電文時觸發 `DownloadReservationModal` 提交申請，不提供即時多筆下載。
*   **身分偵測**: 每一次請求時檢查 `mhh.auth.dev-mode` 與 SSO Token 狀態。

### 2.2 核心視圖 (Views)
*   `DashboardView`: 代辦事項清單 (由 `ApprovalApiController` 提供)。
*   `MessageSearchView`: 電文查詢與 PDF 下載預約 (由 `MessageApiController` 提供)。
*   `ApprovalListView`: 主管審核清單 (含自訂角色與批次下載審核)。

### 2.3 UI 組件設計
*   `AppHeader`: 整合使用者資訊 (User Info)、國別切換 (Country Switcher) 與退出登入。
*   `LoadingSpinner`: 控制全局 Loading 狀態。

## 三、 建置步驟 (Building Steps)
- [ ] **Step 1: 基礎資源配置**
    - 匯入離線版 Vue 3、Bootstrap 5 與 Axios 到 `static/lib`。
    - 建立 Axios 攔截器，處理身份驗證錯誤。
- [ ] **Step 2: 公用組件開發**
    - `AppHeader` (國別與語系)。
    - 全域通知組件 (Toasts/Modals)。
- [ ] **Step 3: 介面功能開發**
    - `DashboardView`: 代辦事項清單及預約下載進度 (由 `DownloadReservationController` 提供進度)。
    - `MessageSearchView`: 電文查詢與預約下載彈窗 (由 `MessageApiController` 提供)。實作 (串接 API 的分頁與預覽功能)。
- [ ] **Step 4: 審核流程建置**
    - `ApprovalListView` 實作 Maker-Checker 的互動逻辑。
    - 確認所有 API 接口與 AP (Backend) 完整串接。
