# SWAL → MHH 欄位 ↔ SWIFT Tag 對照

> 最後更新：2026-04-04

## 架構概覽

```
SWAL (Oracle, 唯讀)                       MHH (MS SQL Server)
──────────────────────────────────         ────────────────────────────────────
FBSWTW_OWNER.SWMIMSG  ──┐                 MSG_INCOMING      ← 搜尋表（快速 Query）
                         ├─ SW_UMID ─→
FBSWTW_OWNER.SWIMTX   ──┘                 MSG_INCOMING_TX   ← 電文表（MT_CONTENT / MX_CONTENT）

FBSWTW_OWNER.SWMOMSG  ──┐                 MSG_OUTGOING      ← 搜尋表
                         ├─ SW_UMID ─→
FBSWTW_OWNER.SWOMTX   ──┘                 MSG_OUTGOING_TX   ← 電文表
```

**同步策略：** `SwalSyncJob` 以 `MSG_DATE`（= `MESG_CREATE_DATE_TIME`）為增量 watermark，每批最多 500 筆；以 `SW_UMID` 去重。  
**欄位來源標示：** `SWMIMSG` = 直接從 SWMIMSG 欄位取值；`SWIMTX` = 從 SWIMTX CLOB 解析；`衍生` = 系統計算。

---

## 進電搜尋欄位對照（MSG_INCOMING ← SWMIMSG）

| MHH 欄位 | SWAL 欄位 | MT Tag | MX XML 元素 | UI 搜尋 | UI 顯示 |
|----------|-----------|--------|-------------|:-------:|:-------:|
| `MESSAGE_ID` | `SW_UMID` | — | — | — | — |
| `MT_TYPE` | `MESG_TYPE` | — | — | ★ | ★ |
| `MX_TYPE` | `MX_MESG_TYPE` | — | — | ★ | ★ |
| `MESSAGE_TYPE` | 衍生（有 MX 取 MX，否則 `"MT"+MT_TYPE`） | — | — | ★ | ★ |
| `SENDER` | `SEND_SWIFT_ADDR` | Block 2 Sender | `<InstgAgt><FinInstnId><BICFI>` | ★ | ★ |
| `RECEIVER` | `RECV_SWIFT_ADDR` | Block 2 Receiver | `<InstdAgt><FinInstnId><BICFI>` | ★ | ★ |
| `UNIT_CODE` | `MESG_UNIT` | — | — | ★ | ★ |
| `AMOUNT` | `TAG_AMT` | Tag 32A（金額部分） | `<IntrBkSttlmAmt>` / `<Amt>` | ★ | ★ |
| `CURRENCY` | `CCY` | Tag 32A（幣別部分） | `<IntrBkSttlmAmt Ccy="...">` | — | ★ |
| `VALUE_DATE` | `VALUE_DATE` | Tag 32A（起息日） | `<IntrBkSttlmDt>` / `<ReqdExctnDt>` | ★ | ★ |
| `REFERENCE` | `SWP_REF_NO_ITEM` | — | — | ★ | ★ |
| `TAG_20` | `TAG_20` | Tag 20 | `<MsgId>` / `<InstrId>` | ★ | ★ |
| `TAG_21` | `TAG_21` | Tag 21 | `<OrgnlMsgId>` / `<RltdRef>` | ★ | ★ |
| `OSN` | `SESSION_NO` + `SEQUENCE_NO`（拼接 10 碼） | Block 1 Session/Seq | — | ★ | ★ |
| `AML_FLAG` | `AML_FLAG` | — | — | ★ | ★ |
| `AML_STATUS` | `AML_RESULT` | — | — | ★ | ★ |
| `FLOW_STATUS` | `HOST_TRANSMIT_STUS` | — | — | ★ | ★ |
| `PDE_FLAG` | `PDE_FLAG` | Block 3 PDE Trailer | — | — | ★ |
| `MSG_DATE` | `MESG_CREATE_DATE_TIME` | — | — | ★ | ★ |
| `SYNC_TIME` | 系統時間 | — | — | — | — |
| `SOURCE` | 系統常數 `"SWAL"` / `"PDF"` | — | — | — | — |

---

## 使用者需求擴充搜尋欄位（MSG_INCOMING 新增）

以下欄位為使用者需求（`docs/user需求.md`），需新增至 `MSG_INCOMING` 搜尋表。  
SWAL `SWMIMSG` 未直接存放這些值，須於同步時從 `SWIMTX` CLOB（MT_CONTENT / MX_CONTENT）解析後寫入。

| MHH 欄位（建議） | MT Tag | MX XML 元素 | 說明 |
|-----------------|--------|-------------|------|
| `ORDERING_CUST_NAME` | Tag 50K / 50F（第二行起） | `<Dbtr><Nm>` | 匯款人名稱 |
| `ORDERING_CUST_ACCT` | Tag 50K（第一行）/ 50A（帳號） | `<DbtrAcct><Id><IBAN>` 或 `<Othr><Id>` | 匯款人帳號 |
| `BENEFICIARY_NAME` | Tag 59 / 59F（第一行名稱行） | `<Cdtr><Nm>` | 受益人名稱 |
| `BENEFICIARY_ACCT` | Tag 59 / 59A（帳號行）| `<CdtrAcct><Id><IBAN>` 或 `<Othr><Id>` | 受益人帳號 |
| `UETR` | Block 3 Tag 121（`SWMIMSG.TAG_121`） | `<UETR>` | Unique End-to-End Transaction Reference |

> **`UETR` 特殊說明：** SWAL 的 `SWMIMSG.TAG_121` 欄位直接存放 UETR 值，不需解析 CLOB；MX 則取 `<UETR>` 元素。

---

## 電文欄位（MSG_INCOMING_TX ← SWIMTX）

| MHH 欄位 | SWAL 欄位 | 說明 |
|----------|-----------|------|
| `MESSAGE_ID` | `SW_UMID` | FK，對應 MSG_INCOMING |
| `MT_CONTENT` | `SWIMTX.MX_MESG_TYPE`（CLOB） | MT 原始 Block 4 電文 |
| `MX_CONTENT` | `SWIMTX.MX_MESG_TYPE`（CLOB） | MX 原始 XML 電文 |
| `SYNC_TIME` | 系統時間 | 電文同步時間 |

> SWIMTX 的 `MX_MESG_TYPE` CLOB 欄位同時存放 MT 與 MX 格式，精確拆分待完整 DDL 確認（`SwalSyncJob` 有 TODO 標記）。

---

## PARAMETERS JSON 欄位

不獨立建搜尋欄，以 JSON 格式存入 `MSG_INCOMING.PARAMETERS`：

| JSON key | SWAL 欄位 | 說明 |
|----------|-----------|------|
| `PDE_TRAILER` | `PDE_TRAILER` | PDE Trailer 內容 |
| `PDM_TRAILER` | `PDM_TRAILER` | PDM Trailer 內容 |
| `AML_TRANSMIT_STUS` | `AML_TRANSMIT_STUS` | AML 傳送狀態 |
| `HOST_TRANSMIT_TYPE` | `HOST_TRANSMIT_TYPE` | 業務系統傳送類型 |
| `MX_MSGID` | `MX_MSGID` | MX 電文 MsgId |
| `MX_REQUEST_TYPE` | `MX_REQUEST_TYPE` | MX 請求類型 |
| `MX_REQUEST_SUBTYPE` | `MX_REQUEST_SUBTYPE` | MX 請求子類型 |
| `MESG_STATUS` | `MESG_STATUS` | 電文狀態碼 |
| `MESG_SERVICE` | `MESG_SERVICE` | 服務識別，預設 `swift.fin` |

---

## 出電差異說明（MSG_OUTGOING ← SWMOMSG）

出電與進電使用相同框架，以下為主要差異：

| 差異點 | 說明 |
|--------|------|
| `OSN` → UI 顯示為 **ISN** | DB 欄位名稱相同，UI 標籤不同 |
| `FLOW_STATUS` 外另有 SAA 狀態 | 出電 UI 顯示「傳送SAA狀態」「ACK/NAK狀態」，待 SWOMSG DDL 確認後補充 |

---

## 相關檔案

| 類型 | 路徑 |
|------|------|
| SWAL DDL（進電搜尋表） | [docs/SWAL_SWMIMSG.sql](../SWAL_SWMIMSG.sql) |
| SWAL DDL（進電電文表） | [docs/SWAL_SWIMTX.sql](../SWAL_SWIMTX.sql) |
| MHH DB Schema | [DB-init/01_schema.sql](../../DB-init/01_schema.sql) |
| JPA 搜尋表 Entity | [SwiftMessageBase.java](../../mhh-common/src/main/java/com/mhh/common/entity/SwiftMessageBase.java) |
| JPA 電文表 Entity | [SwiftMessageTxBase.java](../../mhh-common/src/main/java/com/mhh/common/entity/SwiftMessageTxBase.java) |
| 同步 Job | [SwalSyncJob.java](../../mhh-batch/src/main/java/com/mhh/batch/job/SwalSyncJob.java) |
