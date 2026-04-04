# SWAL ↔ MHH 資料庫對照表

> 最後更新：2026-04-03

## 架構概覽

SWAL（SWIFT Adapter Layer）使用 Oracle DB，MHH 使用 MS SQL Server。
MHH 從 SWAL 唯讀同步，**不回寫任何 SWAL 欄位**，以 `SW_UMID` 為全局唯一鍵。

MHH 採用與 SWAL 相同的**雙表設計**：一張搜尋表（輕量，支援快速查詢），一張電文表（儲存原始電文大欄位，僅在需要電文全文時才存取）。

```
SWAL (Oracle, 唯讀)                     MHH (MS SQL Server)
─────────────────────────────────────   ──────────────────────────────────────────
FBSWTW_OWNER.SWMIMSG  ──┐              MSG_INCOMING      ← 搜尋表（可搜尋欄位 + PARAMETERS）
                         ├─ SW_UMID ─→
FBSWTW_OWNER.SWIMTX   ──┘              MSG_INCOMING_TX   ← 電文表（MT_CONTENT、MX_CONTENT）

FBSWTW_OWNER.SWMOMSG  ──┐              MSG_OUTGOING      ← 搜尋表（同進電框架）
                         ├─ SW_UMID ─→
FBSWTW_OWNER.SWOMTX   ──┘              MSG_OUTGOING_TX   ← 電文表（同進電框架）
```

**兩表以 `MESSAGE_ID`（= `SW_UMID`）對應，`MSG_INCOMING_TX.MESSAGE_ID` 為外鍵。**

| 表 | 對應 SWAL | 查詢頻率 | 儲存內容 |
|----|-----------|---------|---------|
| `MSG_INCOMING` | `SWMIMSG` | 高（列表搜尋） | 可搜尋欄位、PARAMETERS JSON |
| `MSG_INCOMING_TX` | `SWIMTX` | 低（電文詳情） | MT_CONTENT、MX_CONTENT |
| `MSG_OUTGOING` | `SWMOMSG` | 高 | 同上 |
| `MSG_OUTGOING_TX` | `SWOMTX` | 低 | 同上 |

**同步策略：** `SwalSyncJob` 以本系統 `MSG_DATE`（= SWMIMSG.`MESG_CREATE_DATE_TIME`）為增量 watermark，每批最多 500 筆。以 `SW_UMID`（unique key）去重保證冪等。搜尋欄位寫入 `MSG_INCOMING`，原始電文寫入 `MSG_INCOMING_TX`。

---

## SWAL 表說明

| SWAL 表 | 類型 | MHH 對應表 | 說明 |
|---------|------|-----------|------|
| `SWMIMSG` | 進電搜尋表 | `MSG_INCOMING` | 每筆電文一列，含所有可搜尋欄位（類型、金額、AML 狀態等） |
| `SWIMTX` | 進電電文表 | `MSG_INCOMING_TX` | 原始電文 CLOB，MT / MX 內文均存於 `MX_MESG_TYPE` 欄位 |
| `SWMOMSG` | 出電搜尋表 | `MSG_OUTGOING` | 同 SWMIMSG 框架 |
| `SWOMTX` | 出電電文表 | `MSG_OUTGOING_TX` | 同 SWIMTX 框架 |

> **注意：** `SWIMTX.MX_MESG_TYPE` 欄位名稱雖含 MX，但實際存放 MT 與 MX 兩種格式的原始電文內容（CLOB）。MT 電文的原始 Block 4 格式與 MX XML 是否儲存於同一欄位或有其他欄位，待後續 DDL 完整版本確認。

---

## MT ↔ MX 電文互轉對照

SWAL 系統在儲存電文時，已對 MT 與 MX 格式進行互轉，**同一筆 `SW_UMID` 可同時帶有 MT 與 MX 兩種版本**。
MHH 以獨立欄位分別儲存，便於依格式搜尋或取用原始電文：

| MHH 欄位 | 儲存內容 | SWAL 來源 |
|----------|---------|-----------|
| `MT_TYPE` | MT 類型碼，如 `"103"` | `SWMIMSG.MESG_TYPE` |
| `MX_TYPE` | MX 類型碼，如 `"pacs.008.001.08"` | `SWMIMSG.MX_MESG_TYPE`（VARCHAR） |
| `MT_CONTENT` | MT 原始 Block 4 格式電文 | `SWIMTX.MX_MESG_TYPE`（CLOB，MT 部分） |
| `MX_CONTENT` | MX 原始 XML 電文 | `SWIMTX.MX_MESG_TYPE`（CLOB，MX 部分） |

### 進電（SWMIMSG）MT ↔ MX 可對應組合

| MT 類型碼 | MT 電文名稱 | MX 類型碼 | MX 電文名稱 |
|:---------:|------------|:---------:|------------|
| **012** | Delivery Notification | `xsys.003.001.01` | System Message Acknowledgement |
| **019** | Abort Notification | `xsys.003.001.01` | System Message Acknowledgement |
| **103** | Single Customer Credit Transfer | `pacs.008.001.08` | FI to FI Customer Credit Transfer |
| **103** | Single Customer Credit Transfer | `pacs.004.001.09` | Payment Return |
| **105** | EDIFACT Envelope | `camt.105.001.02` | Cash Management Proprietary Data |
| **191** | Free Format（MT1xx 查詢回覆） | `camt.106.001.02` | Cash Management Proprietary Data |
| **199** | Free Format（MT1xx） | `pacs.002.001.10` | Payment Status Report |
| **202** | Financial Institution Transfer | `pacs.004.001.09` | Payment Return |
| **210** | Notice to Receive | `camt.057.001.06` | Notification To Receive |
| **299** | Free Format（MT2xx） | `pacs.002.001.19` | Payment Status Report |
| **910** | Confirmation of Debit | `camt.054.001.08` | Bank to Customer Debit Credit Notification |
| **950** | Statement Message | `camt.053.001.08` | Bank to Customer Statement |

### 出電（SWMOMSG）MT ↔ MX 可對應組合

| MT 類型碼 | MT 電文名稱 | MX 類型碼 | MX 電文名稱 |
|:---------:|------------|:---------:|------------|
| **103** | Single Customer Credit Transfer | `pacs.008.001.08` | FI to FI Customer Credit Transfer |
| **110** | Advice of Cheque(s) | `camt.107.001.01` | Cheque Cancellation Or Stop Request |
| **191** | Free Format（MT1xx 查詢回覆） | `camt.106.001.02` | Cash Management Proprietary Data |
| **199** | Free Format（MT1xx） | `pacs.002.001.10` | Payment Status Report |
| **202** | Financial Institution Transfer | `pacs.004.001.09` | Payment Return |
| **202** | Financial Institution Transfer | `pacs.009.001.08` | Financial Institution Credit Transfer |
| **210** | Notice to Receive | `camt.057.001.06` | Notification To Receive |

---

## 進電欄位對照（SWMIMSG + SWIMTX → MSG_INCOMING + MSG_INCOMING_TX）

### 已建立獨立欄位

| MHH 欄位 | 所在表 | 型別 | SWAL 來源表 | SWAL 欄位 | 說明 |
|----------|-------|------|------------|----------|------|
| `MESSAGE_ID` | MSG_INCOMING | NVARCHAR(100) | SWMIMSG | `SW_UMID` | 全局唯一鍵 |
| `MT_TYPE` | MSG_INCOMING | NVARCHAR(10) | SWMIMSG | `MESG_TYPE` | MT 類型碼，如 `"103"` |
| `MX_TYPE` | MSG_INCOMING | NVARCHAR(50) | SWMIMSG | `MX_MESG_TYPE` | MX 類型碼，如 `"pacs.008.001.09"` |
| `MESSAGE_TYPE` | MSG_INCOMING | NVARCHAR(50) | 衍生 | — | 顯示主類型：有 MX 取 MX，否則 `"MT" + MT_TYPE` |
| `SENDER` | MSG_INCOMING | NVARCHAR(50) | SWMIMSG | `SEND_SWIFT_ADDR` | 發電行 SWIFT 地址（11 碼） |
| `RECEIVER` | MSG_INCOMING | NVARCHAR(50) | SWMIMSG | `RECV_SWIFT_ADDR` | 收電行 SWIFT 地址 |
| `UNIT_CODE` | MSG_INCOMING | NVARCHAR(20) | SWMIMSG | `MESG_UNIT` | 電文單位別 |
| `AMOUNT` | MSG_INCOMING | DECIMAL(18,5) | SWMIMSG | `TAG_AMT` | 金額 |
| `CURRENCY` | MSG_INCOMING | NVARCHAR(3) | SWMIMSG | `CCY` | 幣別 |
| `VALUE_DATE` | MSG_INCOMING | NVARCHAR(8) | SWMIMSG | `VALUE_DATE` | 起息日，YYYYMMDD 字串 |
| `REFERENCE` | MSG_INCOMING | NVARCHAR(100) | SWMIMSG | `SWP_REF_NO_ITEM` | Reference (SWP) |
| `TAG_20` | MSG_INCOMING | NVARCHAR(35) | SWMIMSG | `TAG_20` | SWIFT TAG 20（發報行參考號碼） |
| `TAG_21` | MSG_INCOMING | NVARCHAR(35) | SWMIMSG | `TAG_21` | SWIFT TAG 21（相關參考號碼） |
| `OSN` | MSG_INCOMING | NVARCHAR(20) | SWMIMSG | `SESSION_NO` + `SEQUENCE_NO` | 拼接 OSN，如 `"1234000001"` |
| `AML_FLAG` | MSG_INCOMING | NVARCHAR(1) | SWMIMSG | `AML_FLAG` | AML 需核記旗標 |
| `AML_STATUS` | MSG_INCOMING | NVARCHAR(10) | SWMIMSG | `AML_RESULT` | AML 核態結果 |
| `FLOW_STATUS` | MSG_INCOMING | NVARCHAR(1) | SWMIMSG | `HOST_TRANSMIT_STUS` | in-Flow 傳送業務系統狀態 |
| `PDE_FLAG` | MSG_INCOMING | NVARCHAR(1) | SWMIMSG | `PDE_FLAG` | PDE 旗標 |
| `MSG_DATE` | MSG_INCOMING | DATETIME | SWMIMSG | `MESG_CREATE_DATE_TIME` | 電文建立時間（增量 watermark） |
| `MT_CONTENT` | **MSG_INCOMING_TX** | NVARCHAR(MAX) | SWIMTX | `MX_MESG_TYPE` (CLOB)¹ | MT 原始 Block 4 格式電文 |
| `MX_CONTENT` | **MSG_INCOMING_TX** | NVARCHAR(MAX) | SWIMTX | `MX_MESG_TYPE` (CLOB) | MX 原始 XML 電文 |

> ¹ MT_CONTENT 的 SWIMTX 來源欄位待完整 DDL 確認後更新；目前 `SwalSyncJob` 有 TODO 標記。

### 存入 PARAMETERS JSON（不獨立建欄）

以下欄位以 JSON 格式存入 `PARAMETERS` 欄位，保留供稽核與擴充查詢：

| JSON key | SWAL 來源 | SWAL 欄位 | 說明 |
|----------|-----------|----------|------|
| `PDE_TRAILER` | SWMIMSG | `PDE_TRAILER` | PDE Trailer 內容 |
| `PDM_TRAILER` | SWMIMSG | `PDM_TRAILER` | PDM Trailer 內容 |
| `AML_TRANSMIT_STUS` | SWMIMSG | `AML_TRANSMIT_STUS` | AML 傳送狀態 |
| `HOST_TRANSMIT_TYPE` | SWMIMSG | `HOST_TRANSMIT_TYPE` | 業務系統傳送類型 |
| `MX_MSGID` | SWMIMSG | `MX_MSGID` | MX 電文 MsgId |
| `MX_REQUEST_TYPE` | SWMIMSG | `MX_REQUEST_TYPE` | MX 請求類型 |
| `MX_REQUEST_SUBTYPE` | SWMIMSG | `MX_REQUEST_SUBTYPE` | MX 請求子類型 |
| `MESG_STATUS` | SWMIMSG | `MESG_STATUS` | 電文狀態碼 |
| `MESG_SERVICE` | SWMIMSG | `MESG_SERVICE` | 服務識別，預設 `swift.fin` |

### 未同步至 MHH 的 SWAL 欄位

以下欄位為 SWAL 系統內部欄位，不在 MHH 儲存範圍：

| SWAL 欄位 | 說明 | 不同步原因 |
|-----------|------|-----------|
| `MESG_SESSION_HOLDER` | SWAL 作業 Session | SWAL 內部資訊 |
| `MESG_CREATE_DATE` | 電文日期（VARCHAR） | 與 `MESG_CREATE_DATE_TIME` 重複 |
| `VALIDATION_FLAG` | 驗證旗標 | SWAL 內部驗證 |
| `COPY_SERVICE_CODE` | 複製服務碼 | SWAL 路由資訊 |
| `NEW_PRIORITY` | 優先等級 | 不影響 MHH 查詢 |
| `SWIFT_BLOCK3` | Block 3 全文 | 已在 MT_CONTENT 中 |
| `SWIFT_MUR` | MUR 參考號碼 | 非主要查詢欄位 |
| `SEND_SWIFT_LT` / `RECV_SWIFT_LT` | LT 碼（1 碼） | 已含於 SWIFT Address |
| `NET_INPUTTIME` / `NET_OUTPUTTIME` / `NET_MIR` | SWIFT 網路時間戳 | SWAL 網路層資訊 |
| `BRANCH` | 分行代碼 | 由 UNIT_CODE 涵蓋 |
| `VERSION_NO` | 版本號 | SWAL 版控 |
| `MESG_CHECKSUM` | 電文校驗碼 | SWAL 完整性驗證 |
| `CBT_MQ_MSGID_HEX` / `CBT_SUMID` / `CBT_RECV_DATE_TIME` | CBT（Core Banking）傳輸 ID | 核心銀行系統內部 |
| `AML_SEND_DATE_TIME` / `AML_HIT_RESULT` / `AML_HIT_DATE_TIME` / `AML_RESULT_DATE_TIME` | AML 時間戳 & 命中細項 | AML_FLAG/STATUS 已足夠搜尋 |
| `HOST_SEND_DATE_TIME` | 業務系統傳送時間 | 狀態欄位補充資訊 |
| `LAST_UPDATE_TIME` | SWAL 最後更新時間 | 使用 `MESG_CREATE_DATE_TIME` 做 watermark |
| `MX_DATAMAP_RESULT_FLAG` / `_CODE` / `_DESC` | MX DataMap 轉換結果 | SWAL 內部轉換狀態 |
| `TAG_121` / `TAG_111` | SWIFT TAG 特殊欄位 | 非常用查詢欄位 |
| `MX_SWIFT_REF` | MX SWIFT Reference | MX_MSGID 已涵蓋 |
| `INFO_DATA_FIELD_01~05` | 擴充資料欄位 | 目前無業務需求 |

---

## 出電欄位對照（SWMOMSG + SWOMTX → MSG_OUTGOING）

出電表結構與進電相同框架，欄位對應規則一致：

| 進電 SWAL 來源 | 出電 SWAL 對應 | 說明 |
|---------------|--------------|------|
| SWMIMSG | SWMOMSG | 搜尋欄位表（同欄位框架） |
| SWIMTX | SWOMTX | 原始電文表（同欄位框架） |

> SWOMSG / SWOMTX 的完整 DDL 尚未取得，俟確認後補充。

---

## MSG_INCOMING / MSG_OUTGOING 欄位總覽

```
MSG_INCOMING（搜尋表）                      MSG_INCOMING_TX（電文表）
├── 識別                                    ├── MESSAGE_ID  ← SW_UMID（FK）
│   ├── MESSAGE_ID   ← SW_UMID (PK ref)    ├── MT_CONTENT  ← SWIMTX CLOB（MT 格式）
│   ├── MT_TYPE      ← MESG_TYPE           └── MX_CONTENT  ← SWIMTX CLOB（MX 格式）
│   ├── MX_TYPE      ← MX_MESG_TYPE (VAR)
│   └── MESSAGE_TYPE ← 衍生顯示值
├── 路由
│   ├── SENDER       ← SEND_SWIFT_ADDR
│   ├── RECEIVER     ← RECV_SWIFT_ADDR
│   └── UNIT_CODE    ← MESG_UNIT
├── 金融
│   ├── AMOUNT       ← TAG_AMT
│   ├── CURRENCY     ← CCY
│   └── VALUE_DATE   ← VALUE_DATE
├── 參考
│   ├── REFERENCE    ← SWP_REF_NO_ITEM
│   ├── TAG_20
│   ├── TAG_21
│   └── OSN          ← SESSION_NO + SEQUENCE_NO
├── 狀態
│   ├── AML_FLAG     ← AML_FLAG
│   ├── AML_STATUS   ← AML_RESULT
│   ├── FLOW_STATUS  ← HOST_TRANSMIT_STUS
│   └── PDE_FLAG
├── 時間
│   ├── MSG_DATE     ← MESG_CREATE_DATE_TIME  (watermark)
│   └── SYNC_TIME    ← 同步當下時間
└── 彈性 JSON
    └── PARAMETERS   ← PDE_TRAILER, PDM_TRAILER,
                        AML_TRANSMIT_STUS, HOST_TRANSMIT_TYPE,
                        MX_MSGID, MX_REQUEST_TYPE, MX_REQUEST_SUBTYPE,
                        MESG_STATUS, MESG_SERVICE

MSG_OUTGOING / MSG_OUTGOING_TX 結構與上相同。
```

---

## 相關檔案

| 類型 | 路徑 |
|------|------|
| SWAL DDL（進電搜尋表） | [docs/SWAL_SWMIMSG.sql](SWAL_SWMIMSG.sql) |
| SWAL DDL（進電電文表） | [docs/SWAL_SWIMTX.sql](SWAL_SWIMTX.sql) |
| MT↔MX 對照（進電） | [docs/SWMIMSG_MTMX對照.md](SWMIMSG_MTMX對照.md) |
| MT↔MX 對照（出電） | [docs/SWOMSG_MTMX對照.md](SWOMSG_MTMX對照.md) |
| MHH DB Schema | [DB-init/01_schema.sql](../DB-init/01_schema.sql) |
| JPA 搜尋表 Entity | [mhh-common/.../SwiftMessageBase.java](../mhh-common/src/main/java/com/mhh/common/entity/SwiftMessageBase.java) |
| JPA 電文表 Entity | [mhh-common/.../SwiftMessageTxBase.java](../mhh-common/src/main/java/com/mhh/common/entity/SwiftMessageTxBase.java) |
| 同步 Job | [mhh-batch/.../SwalSyncJob.java](../mhh-batch/src/main/java/com/mhh/batch/job/SwalSyncJob.java) |
