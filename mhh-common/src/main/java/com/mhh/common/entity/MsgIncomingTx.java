package com.mhh.common.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

/** 進電電文表 → 對應資料表 MSG_INCOMING_TX（對齊 SWAL: SWIMTX） */
@Entity
@Table(name = "MSG_INCOMING_TX")
@NoArgsConstructor
public class MsgIncomingTx extends SwiftMessageTxBase {
}
