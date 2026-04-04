# 7_msg — SWIFT 電文參考文件

本資料夾整合 MHH 系統所有 SWIFT 電文相關規格，取代原 `6_MTMXtype.md`、`8_swal_mapping.md`、`SWMIMSG_MTMX對照.md`、`SWOMSG_MTMX對照.md` 及進出電文截圖資料夾。

## 文件索引

| 檔案 | 內容 |
|------|------|
| [1_message_types.md](1_message_types.md) | 總類型欄位表：所有 MT / MX 電文類型與重要解析欄位（★必填 / ☆選填） |
| [2_mtmx_mapping.md](2_mtmx_mapping.md) | MT ↔ MX 對照表：進電（SWMIMSG）與出電（SWMOMSG）可合併對應組合 |
| [3_swal_mhh_mapping.md](3_swal_mhh_mapping.md) | SWAL → MHH 欄位轉換對照，含搜尋欄位、PARAMETERS JSON、未同步欄位說明 |

## UI 截圖

| 截圖 | 說明 |
|------|------|
| [img/進電搜尋欄位.PNG](img/進電搜尋欄位.PNG) | 進電查詢介面搜尋條件列 |
| [img/進電顯示欄位.PNG](img/進電顯示欄位.PNG) | 進電列表顯示欄位（前段） |
| [img/進電顯示欄位2.PNG](img/進電顯示欄位2.PNG) | 進電列表顯示欄位（後段：PDE/PDM/業務系統） |
| [img/出電搜尋欄位.PNG](img/出電搜尋欄位.PNG) | 出電查詢介面搜尋條件列 |
| [img/出電顯示欄位.PNG](img/出電顯示欄位.PNG) | 出電列表顯示欄位（前段） |
| [img/出電顯示欄位2.PNG](img/出電顯示欄位2.PNG) | 出電列表顯示欄位（後段：SAA/ACK/NAK/業務系統） |

## 資料來源

- SWAL（Oracle）：`FBSWTW_OWNER.SWMIMSG` / `SWIMTX`（進電）、`SWMOMSG` / `SWOMTX`（出電）
- MHH（MS SQL Server）：`MSG_INCOMING` / `MSG_INCOMING_TX`、`MSG_OUTGOING` / `MSG_OUTGOING_TX`
- MHH Schema：[DB-init/01_schema.sql](../../DB-init/01_schema.sql)
