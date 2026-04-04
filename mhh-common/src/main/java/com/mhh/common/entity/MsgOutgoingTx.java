package com.mhh.common.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

/** 出電電文表 → 對應資料表 MSG_OUTGOING_TX（對齊 SWAL: SWOMTX） */
@Entity
@Table(name = "MSG_OUTGOING_TX")
@NoArgsConstructor
public class MsgOutgoingTx extends SwiftMessageTxBase {
}
