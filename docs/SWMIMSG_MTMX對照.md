# SWMIMSG MT ↔ MX 可對應清單（進電）

> 來源：SWAL `FBSWTW_OWNER.SWMIMSG`
> 最後更新：2026-04-03

SWAL 系統對進電（Incoming）的 MT 電文已完成 MT↔MX 互轉，
以下列出 `MESG_TYPE`（MT 類型碼）與 `MX_MESG_TYPE`（MX 類型碼）的可對應組合。
同一 MT 類型碼可能對應多種 MX 類型（視電文用途而定）。

---

## 對照表

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

---

## 備註

- MT **012 / 019**：系統訊息（ACK/NAK），對應 ISO 20022 系統訊息 `xsys.003`
- MT **103** 同時對應 `pacs.008`（正常進電）與 `pacs.004`（款項退回）兩種情境
- MT **202** 進電對應 `pacs.004`（退款情境）；出電另有 `pacs.009` 正常匯款對應（見 SWOMSG 對照）
- MT **191 / 199 / 299**：Free Format 依 MT 系列對應不同 camt / pacs 類型

## 相關文件

- [SWOMSG_MTMX對照.md](SWOMSG_MTMX對照.md) — 出電對照
- [8_swal_mapping.md](8_swal_mapping.md) — SWAL ↔ MHH 欄位完整對照
- [6_MTMXtype.md](6_MTMXtype.md) — MT/MX 電文重要欄位說明
