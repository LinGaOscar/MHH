# MHH 開發待辦事項

> 最後更新：2026-04-01
> 本文件追蹤尚未完成或等待外部資訊的開發項目。

---

## Batch — SwalSyncJob

### 🔴 阻擋中：等待 SWAL DDL

以下項目**無法進行**，需先取得 SWAL 系統兩個 TABLE 的 DDL。

**預期 SWAL DB 結構：**
- **TABLE 1**：可搜尋欄位表 — `MSG_UID`（主鍵）+ 結構化可搜尋欄位（如 `TAG20`、MT/MX 類型、金額、幣別等）
- **TABLE 2**：原始電文表 — `MSG_UID`（FK）+ MT 原始電文欄位 + MX 原始電文欄位

| # | 項目 | 說明 |
|---|------|------|
| S-1 | **更新 `SwalSyncJob` SQL 為 JOIN 兩個 TABLE** | 目前 SQL 為 placeholder（單表查詢），需改為 `TABLE1 JOIN TABLE2 ON MSG_UID`，確認實際表名與欄位名後實作 |
| S-2 | **更新 `processRow()` 欄位對應** | 目前假設欄位為 `MSG_ID / MSG_TYPE / SENDER / RECEIVER / RAW_CONTENT`，須對應實際欄位名（如 `MSG_UID / TAG20 / MT原始欄位 / MX原始欄位`） |
| S-3 | **確認 `SYNC_STATUS` 欄位所在 TABLE** | 回寫已同步狀態（`SYNC_STATUS = 'Y'`）要對到正確的表，目前預設在 TABLE1 |
| S-4 | **確認 MT 原始電文欄位名稱** | TABLE2 中存放 MT 原始電文（SWIFT Block 4 格式）的欄位名 |
| S-5 | **確認 MX 原始電文欄位名稱** | TABLE2 中存放 MX 原始 XML 的欄位名 |

---

### 🟡 設計決策（DDL 到後確認）

| # | 問題 | 選項 |
|---|------|------|
| S-6 | **SwalSyncJob 是否需要 Parser？** | 若 TABLE1 已有 tag20、類型、金額等完整可搜尋欄位 → 直接欄位 mapping，**不需要 Parser**；若欄位不完整，才對 TABLE2 的原始電文呼叫 `MtGenericParser` / `MxGenericParser` 補齊 |
| S-7 | **MX 原始電文格式** | 若 TABLE2 存的是完整 XML → `MxGenericParser` 的 XML tag 策略可直接使用；若只是格式化報表文字 → 需調整 regex 策略 |
| S-8 | **MT 原始電文格式** | 確認是否為標準 SWIFT Block 4（`:TAG:` 結構），決定 `MtGenericParser` 能否直接沿用 |

---

## Batch — PdfImportJob

### 🟢 可立即進行

| # | 項目 | 說明 |
|---|------|------|
| P-1 | **補上 save 前的 dedup 檢查** | 目前 PDF 沒有在 `repository.save()` 前先呼叫 `findByMessageId()` 確認，重複 import 會因 `MESSAGE_ID` unique key 拋例外；應改為先查詢，已存在則跳過並 archive |

---

## Parser 框架（mhh-core）

### 🟢 可立即進行

| # | 項目 | 說明 |
|---|------|------|
| C-1 | **依實際 PDF 樣本調整 `MtGenericParser` regex** | 目前 `:32A:` 解析、party 欄位 regex 為通用實作，需以真實 PDF 輸出內容驗證並微調 |
| C-2 | **依實際 PDF 樣本調整 `MxGenericParser` regex** | 同上，XML tag 策略與 label 策略需以真實 MX PDF 輸出驗證 |

### 🟡 視需求決定

| # | 項目 | 說明 |
|---|------|------|
| C-3 | **新增 MT202 Specific Parser** | MT202 為金融機構間匯款，欄位（`:58A:`受益機構）與 MT103 差異較大，若通用解析結果不夠精準可建立專屬 Parser（priority=50） |
| C-4 | **新增 MT700 Specific Parser** | 信用狀電文欄位複雜（`:45A:`貨物、`:46A:`要求文件），視業務需求決定是否需精確解析 |
| C-5 | **新增 MT940/MT950 Specific Parser** | 對帳單電文包含多筆 `:61:` Statement Line，通用 Parser 只抓第一筆，若需完整解析需專屬實作 |

---

## 備註

- Parser 優先順序規則：**priority 數字越小，越優先**
  - Specific Parser（如 `Mt103Parser`）：priority = **50**
  - Generic Parser（`MtGenericParser` / `MxGenericParser`）：priority = **200**（後備）
- SWAL DDL 取得後，請更新本文件 S-1 至 S-8 的狀態
