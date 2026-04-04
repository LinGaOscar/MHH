package com.mhh.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 進電 / 出電電文表共用欄位（原始電文），對應 SWAL 的 SWIMTX / SWOMTX。
 * 以 MESSAGE_ID（= SW_UMID）對應搜尋表 MSG_INCOMING / MSG_OUTGOING。
 *
 * 與搜尋表分離的原因：
 *   避免列表查詢（搜尋）時載入大型 NVARCHAR(MAX) 欄位，提升查詢效能。
 *   僅在使用者點開電文詳情時才 JOIN 或單獨查詢電文表。
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class SwiftMessageTxBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 對應 MSG_INCOMING / MSG_OUTGOING 的 MESSAGE_ID（= SW_UMID） */
    @Column(name = "MESSAGE_ID", unique = true, nullable = false)
    private String messageId;

    /** MT 原始 Block 4 格式電文（SWAL: SWIMTX.MX_MESG_TYPE CLOB，MT 部分） */
    @Lob
    @Column(name = "MT_CONTENT")
    private String mtContent;

    /** MX 原始 XML 電文（SWAL: SWIMTX.MX_MESG_TYPE CLOB，MX 部分） */
    @Lob
    @Column(name = "MX_CONTENT")
    private String mxContent;

    @Column(name = "SYNC_TIME")
    private LocalDateTime syncTime;
}
