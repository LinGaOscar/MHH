package com.mhh.common.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

/** 進電（對本行收到的 SWIFT 電文）→ 對應資料表 MSG_INCOMING */
@Entity
@Table(name = "MSG_INCOMING")
@NoArgsConstructor
public class MsgIncoming extends SwiftMessageBase {
}
