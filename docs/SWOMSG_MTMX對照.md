# SWOMSG MT ↔ MX 可對應清單（出電）

> 來源：SWAL `FBSWTW_OWNER.SWMOMSG`
> 最後更新：2026-04-03

SWAL 系統對出電（Outgoing）的 MT 電文已完成 MT↔MX 互轉，
以下列出 `MESG_TYPE`（MT 類型碼）與 `MX_MESG_TYPE`（MX 類型碼）的可對應組合。
同一 MT 類型碼可能對應多種 MX 類型（視電文用途而定）。

---

## 對照表

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

## 備註

- MT **103** 出電：對應 `pacs.008`（對外發送客戶匯款）
- MT **110** 出電：對應 `camt.107`（支票相關通知）
- MT **202** 出電同時對應兩種 MX：
  - `pacs.009.001.08`：正常行際資金調撥（Financial Institution Credit Transfer）
  - `pacs.004.001.09`：款項退回（Payment Return）
- MT **191 / 199**：Free Format 回覆訊息，對應 camt.106 / pacs.002

## 進出電差異比較

| MT | 進電（SWMIMSG）MX 對應 | 出電（SWMOMSG）MX 對應 |
|----|----------------------|----------------------|
| 103 | pacs.008、pacs.004 | pacs.008 |
| 191 | camt.106 | camt.106 |
| 199 | pacs.002 | pacs.002 |
| 202 | pacs.004 | pacs.004、pacs.009 |
| 210 | camt.057 | camt.057 |
| 012/019 | xsys.003 | —（進電專屬系統訊息） |
| 105 | camt.105 | —（進電專屬） |
| 299 | pacs.002 | —（進電專屬） |
| 910 | camt.054 | —（進電專屬） |
| 950 | camt.053 | —（進電專屬） |
| 110 | — | camt.107（出電專屬） |

## 相關文件

- [SWMIMSG_MTMX對照.md](SWMIMSG_MTMX對照.md) — 進電對照
- [8_swal_mapping.md](8_swal_mapping.md) — SWAL ↔ MHH 欄位完整對照
- [6_MTMXtype.md](6_MTMXtype.md) — MT/MX 電文重要欄位說明
