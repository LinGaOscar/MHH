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
 *
 * SWAL 表對應關係：
 *   SWMIMSG / SWMOMSG → 搜尋欄位（TAG_20/21、CCY、AML 等）
 *   SWIMTX  / SWOMTX  → 原始電文（MT_CONTENT、MX_CONTENT），以 SW_UMID 關聯
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class SwiftMessageBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── 識別 ──────────────────────────────────────────────────────────────────
    /** SW_UMID（SWAL）或 PDF 檔名 */
    @Column(name = "MESSAGE_ID", unique = true)
    private String messageId;

    /** MT 類型碼，如 "103"、"202"（SWAL: SWMIMSG.MESG_TYPE） */
    @Column(name = "MT_TYPE", length = 10)
    private String mtType;

    /** MX 類型碼，如 "pacs.008.001.09"（SWAL: SWMIMSG.MX_MESG_TYPE） */
    @Column(name = "MX_TYPE", length = 50)
    private String mxType;

    /**
     * 顯示用主類型：有 MX 取 MX（如 "pacs.008.001.09"），否則 "MT" + mtType（如 "MT103"）。
     * PDF 來源由各 Parser 直接填入。
     */
    @Column(name = "MESSAGE_TYPE", length = 50)
    private String messageType;

    // ── 路由 ──────────────────────────────────────────────────────────────────
    /** SWAL: SEND_SWIFT_ADDR */
    @Column(name = "SENDER", length = 50)
    private String sender;

    /** SWAL: RECV_SWIFT_ADDR */
    @Column(name = "RECEIVER", length = 50)
    private String receiver;

    /** SWAL: MESG_UNIT */
    @Column(name = "UNIT_CODE", length = 20)
    private String unitCode;

    // ── 金融 ──────────────────────────────────────────────────────────────────
    /** SWAL: TAG_AMT */
    @Column(name = "AMOUNT", precision = 18, scale = 5)
    private BigDecimal amount;

    /** SWAL: CCY */
    @Column(name = "CURRENCY", length = 3)
    private String currency;

    /** SWAL: VALUE_DATE（YYYYMMDD 字串） */
    @Column(name = "VALUE_DATE", length = 8)
    private String valueDate;

    // ── 參考 ──────────────────────────────────────────────────────────────────
    /** SWAL: SWP_REF_NO_ITEM */
    @Column(name = "REFERENCE", length = 100)
    private String reference;

    /** SWAL: TAG_20 */
    @Column(name = "TAG_20", length = 35)
    private String tag20;

    /** SWAL: TAG_21 */
    @Column(name = "TAG_21", length = 35)
    private String tag21;

    /** SWAL: SESSION_NO + SEQUENCE_NO 拼接（10 碼） */
    @Column(name = "OSN", length = 20)
    private String osn;

    // ── 狀態 ──────────────────────────────────────────────────────────────────
    /** SWAL: AML_FLAG */
    @Column(name = "AML_FLAG", length = 1)
    private String amlFlag;

    /** SWAL: AML_RESULT */
    @Column(name = "AML_STATUS", length = 10)
    private String amlStatus;

    /** SWAL: HOST_TRANSMIT_STUS（in-Flow 狀態） */
    @Column(name = "FLOW_STATUS", length = 1)
    private String flowStatus;

    /** SWAL: PDE_FLAG */
    @Column(name = "PDE_FLAG", length = 1)
    private String pdeFlag;

    // ── 時間 ──────────────────────────────────────────────────────────────────
    /** SWAL: MESG_CREATE_DATE_TIME，增量同步 watermark */
    @Column(name = "MSG_DATE")
    private LocalDateTime msgDate;

    @Column(name = "SYNC_TIME")
    private LocalDateTime syncTime;

    /** SWAL | PDF */
    @Column(name = "SOURCE", length = 20)
    private String source;

    // ── 原始電文（暫存，不對應資料庫欄位）────────────────────────────────────
    // 實際持久化至 MSG_INCOMING_TX / MSG_OUTGOING_TX（見 SwiftMessageTxBase）
    /** MT 原始 Block 4 格式電文；Parser / SwalSyncJob 填入後由 Job 轉存至 TX 表 */
    @Transient
    private String mtContent;

    /** MX 原始 XML 電文；Parser / SwalSyncJob 填入後由 Job 轉存至 TX 表 */
    @Transient
    private String mxContent;

    // ── 彈性 JSON ────────────────────────────────────────────────────────────
    /** PDE_TRAILER, PDM_TRAILER, AML_TRANSMIT_STUS, MX_MSGID 等不需獨立搜尋的欄位 */
    @Lob
    @Column(name = "PARAMETERS")
    private String parameters;
}
