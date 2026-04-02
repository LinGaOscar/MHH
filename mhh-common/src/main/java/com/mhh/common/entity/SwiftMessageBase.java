package com.mhh.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 進電 / 出電共用欄位，透過 @MappedSuperclass 分別對應
 * {@link MsgIncoming}（MSG_INCOMING）與 {@link MsgOutgoing}（MSG_OUTGOING）兩張表。
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class SwiftMessageBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "MESSAGE_ID", unique = true)
    private String messageId;

    @Column(name = "MESSAGE_TYPE", length = 50)
    private String messageType;

    @Column(name = "SENDER", length = 50)
    private String sender;

    @Column(name = "RECEIVER", length = 50)
    private String receiver;

    @Column(name = "AMOUNT", precision = 18, scale = 5)
    private BigDecimal amount;

    @Column(name = "CURRENCY", length = 3)
    private String currency;

    @Column(name = "REFERENCE", length = 100)
    private String reference;

    @Lob
    @Column(name = "CONTENT")
    private String content;

    @Lob
    @Column(name = "PARAMETERS")
    private String parameters;

    @Column(name = "SYNC_TIME")
    private LocalDateTime syncTime;

    @Column(name = "SOURCE", length = 20)
    private String source; // SWAL, PDF
}
