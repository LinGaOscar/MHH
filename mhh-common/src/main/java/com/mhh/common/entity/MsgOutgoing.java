package com.mhh.common.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

/** 出電（本行發出的 SWIFT 電文）→ 對應資料表 MSG_OUTGOING */
@Entity
@Table(name = "MSG_OUTGOING")
@NoArgsConstructor
public class MsgOutgoing extends SwiftMessageBase {
}
