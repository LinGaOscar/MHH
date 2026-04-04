package com.mhh.batch.job;

import com.mhh.common.entity.MsgIncoming;
import com.mhh.common.entity.MsgIncomingTx;
import com.mhh.common.repository.MsgIncomingRepository;
import com.mhh.common.repository.MsgIncomingTxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 從 SWAL (Oracle, 唯讀) 增量同步進電到本系統 MSG_INCOMING。
 *
 * SWAL 表結構（兩張表以 SW_UMID 對應）：
 *   FBSWTW_OWNER.SWMIMSG — 可搜尋欄位（TAG_20/21、CCY、AML 等）
 *   FBSWTW_OWNER.SWIMTX  — 原始電文 CLOB（MX_MESG_TYPE 欄位存放 MT 或 MX 原始內容）
 *
 * 同步策略：
 *   - 以本系統 MSG_INCOMING.MSG_DATE（= SWMIMSG.MESG_CREATE_DATE_TIME）為 watermark
 *   - 每次撈 MESG_CREATE_DATE_TIME > watermark 的最多 500 筆，依 MESG_CREATE_DATE_TIME ASC
 *   - 以 SW_UMID（= MESSAGE_ID unique key）去重，已存在則跳過
 *   - SWAL 為唯讀，不回寫任何欄位
 */
@Component("swalSyncJob")
@Slf4j
public class SwalSyncJob implements MhhJob {

    private static final int BATCH_SIZE = 500;

    /**
     * 預設起始 watermark：若本系統無任何 SWAL 資料，從幾天前開始撈。
     * 實際上線時可依需求調整。
     */
    private static final int DEFAULT_LOOKBACK_DAYS = 7;

    private static final String QUERY_SQL =
            "SELECT m.SW_UMID, " +
            "       m.MESG_TYPE, m.MX_MESG_TYPE AS MX_TYPE, m.MESG_SERVICE, " +
            "       m.TAG_20, m.TAG_21, " +
            "       m.CCY, m.TAG_AMT, m.VALUE_DATE, " +
            "       m.SEND_SWIFT_ADDR, m.RECV_SWIFT_ADDR, " +
            "       m.SESSION_NO, m.SEQUENCE_NO, " +
            "       m.MESG_CREATE_DATE_TIME, " +
            "       m.MESG_UNIT, m.MESG_STATUS, " +
            "       m.PDE_FLAG, m.PDE_TRAILER, m.PDM_TRAILER, " +
            "       m.AML_FLAG, m.AML_RESULT, m.AML_TRANSMIT_STUS, " +
            "       m.HOST_TRANSMIT_STUS, m.HOST_TRANSMIT_TYPE, " +
            "       m.SWP_REF_NO_ITEM, " +
            "       m.MX_MSGID, m.MX_REQUEST_TYPE, m.MX_REQUEST_SUBTYPE, " +
            "       t.MX_MESG_TYPE AS RAW_CONTENT " +
            "FROM FBSWTW_OWNER.SWMIMSG m " +
            "LEFT JOIN FBSWTW_OWNER.SWIMTX t ON t.SW_UMID = m.SW_UMID " +
            "WHERE m.MESG_CREATE_DATE_TIME > ? " +
            "ORDER BY m.MESG_CREATE_DATE_TIME ASC " +
            "FETCH FIRST " + BATCH_SIZE + " ROWS ONLY";

    private final JdbcTemplate swalJdbcTemplate;
    private final MsgIncomingRepository incomingRepository;
    private final MsgIncomingTxRepository incomingTxRepository;

    public SwalSyncJob(@Qualifier("swalJdbcTemplate") JdbcTemplate swalJdbcTemplate,
                       MsgIncomingRepository incomingRepository,
                       MsgIncomingTxRepository incomingTxRepository) {
        this.swalJdbcTemplate = swalJdbcTemplate;
        this.incomingRepository = incomingRepository;
        this.incomingTxRepository = incomingTxRepository;
    }

    @Override
    public String getJobName() {
        return "SwalSyncJob";
    }

    @Override
    public void execute() {
        log.info("Starting SwalSyncJob...");

        LocalDateTime watermark = resolveWatermark();
        log.info("SwalSyncJob watermark: {}", watermark);

        List<Map<String, Object>> rows =
                swalJdbcTemplate.queryForList(QUERY_SQL, Timestamp.valueOf(watermark));
        log.info("Found {} candidate rows in SWAL after watermark.", rows.size());

        int saved = 0;
        int skipped = 0;

        for (Map<String, Object> row : rows) {
            String swUmid = asString(row, "SW_UMID");
            if (swUmid == null) {
                log.warn("Skipping row with null SW_UMID.");
                continue;
            }

            if (incomingRepository.findByMessageId(swUmid).isPresent()) {
                log.debug("Message {} already exists, skipping.", swUmid);
                skipped++;
                continue;
            }

            try {
                incomingRepository.save(buildEntity(swUmid, row));

                // 原始電文另存 TX 表（對齊 SWAL: SWIMTX）
                String rawContent = extractClob(row, "RAW_CONTENT");
                if (rawContent != null) {
                    MsgIncomingTx tx = new MsgIncomingTx();
                    tx.setMessageId(swUmid);
                    tx.setMxContent(rawContent);
                    // tx.setMtContent(...); // TODO: 確認 SWIMTX MT 欄位名稱後補上
                    tx.setSyncTime(LocalDateTime.now());
                    incomingTxRepository.save(tx);
                }

                saved++;
                log.debug("Synced message {} from SWAL.", swUmid);
            } catch (Exception e) {
                log.error("Failed to sync message {}: {}", swUmid, e.getMessage(), e);
            }
        }

        log.info("SwalSyncJob completed. Fetched: {}, Saved: {}, Skipped(dup): {}",
                rows.size(), saved, skipped);
    }

    // ── Watermark ─────────────────────────────────────────────────────

    private LocalDateTime resolveWatermark() {
        return incomingRepository.findMaxMsgDateForSwal()
                .orElseGet(() -> LocalDateTime.now().minusDays(DEFAULT_LOOKBACK_DAYS));
    }

    // ── Entity mapping ────────────────────────────────────────────────

    private MsgIncoming buildEntity(String swUmid, Map<String, Object> row) {
        MsgIncoming msg = new MsgIncoming();

        msg.setMessageId(swUmid);

        // ── 電文類型（MT 與 MX 均保留，支援 MT↔MX 互轉配對）──────────────
        String mtTypeCode = asString(row, "MESG_TYPE");   // e.g. "103"
        String mxTypeCode = asString(row, "MX_TYPE");     // e.g. "pacs.008.001.09"
        msg.setMtType(mtTypeCode);
        msg.setMxType(mxTypeCode);
        msg.setMessageType(resolveMessageType(mtTypeCode, mxTypeCode));

        // ── 路由 ──────────────────────────────────────────────────────────
        msg.setSender(asString(row, "SEND_SWIFT_ADDR"));
        msg.setReceiver(asString(row, "RECV_SWIFT_ADDR"));
        msg.setUnitCode(asString(row, "MESG_UNIT"));

        // ── 金融 ──────────────────────────────────────────────────────────
        msg.setAmount(asBigDecimal(row, "TAG_AMT"));
        msg.setCurrency(asString(row, "CCY"));
        msg.setValueDate(asString(row, "VALUE_DATE"));

        // ── 參考 ──────────────────────────────────────────────────────────
        msg.setReference(asString(row, "SWP_REF_NO_ITEM"));
        msg.setTag20(asString(row, "TAG_20"));
        msg.setTag21(asString(row, "TAG_21"));
        msg.setOsn(buildOsn(row));

        // ── 狀態 ──────────────────────────────────────────────────────────
        msg.setAmlFlag(asString(row, "AML_FLAG"));
        msg.setAmlStatus(asString(row, "AML_RESULT"));
        msg.setFlowStatus(asString(row, "HOST_TRANSMIT_STUS"));
        msg.setPdeFlag(asString(row, "PDE_FLAG"));

        // ── 時間 ──────────────────────────────────────────────────────────
        msg.setMsgDate(asLocalDateTime(row, "MESG_CREATE_DATE_TIME"));
        msg.setSyncTime(LocalDateTime.now());
        msg.setSource("SWAL");

        // ── 原始電文（SWIMTX） ────────────────────────────────────────────
        // RAW_CONTENT = SWIMTX.MX_MESG_TYPE CLOB（同時存放 MX XML；MT 欄位待 DDL 確認後補上）
        msg.setMxContent(extractClob(row, "RAW_CONTENT"));
        // msg.setMtContent(extractClob(row, "MT_RAW_CONTENT")); // TODO: 確認 SWIMTX MT 欄位名稱

        msg.setParameters(buildParameters(row));

        return msg;
    }

    /**
     * 顯示用主類型：有 MX 優先（如 "pacs.008.001.09"），否則 "MT" + typeCode（如 "MT103"）。
     */
    private String resolveMessageType(String mtTypeCode, String mxTypeCode) {
        if (mxTypeCode != null && !mxTypeCode.isBlank()) return mxTypeCode;
        return (mtTypeCode != null && !mtTypeCode.isBlank()) ? "MT" + mtTypeCode : null;
    }

    /**
     * OSN = SESSION_NO（4位）+ SEQUENCE_NO（6位）拼接，例如 "1234000001"
     */
    private String buildOsn(Map<String, Object> row) {
        String session  = asString(row, "SESSION_NO");
        String sequence = asString(row, "SEQUENCE_NO");
        if (session == null && sequence == null) return null;
        return (session != null ? session : "") + (sequence != null ? sequence : "");
    }

    /**
     * 無獨立欄位的額外資訊序列化為 JSON，供日後需要時查閱。
     * 包含：PDE_TRAILER, PDM_TRAILER, AML_TRANSMIT_STUS, HOST_TRANSMIT_TYPE,
     *       MX_MSGID, MX_REQUEST_TYPE, MX_REQUEST_SUBTYPE, MESG_STATUS, MESG_SERVICE
     */
    private String buildParameters(Map<String, Object> row) {
        StringBuilder sb = new StringBuilder("{");
        appendJsonField(sb, "PDE_TRAILER",        asString(row, "PDE_TRAILER"),        true);
        appendJsonField(sb, "PDM_TRAILER",         asString(row, "PDM_TRAILER"),        false);
        appendJsonField(sb, "AML_TRANSMIT_STUS",   asString(row, "AML_TRANSMIT_STUS"), false);
        appendJsonField(sb, "HOST_TRANSMIT_TYPE",  asString(row, "HOST_TRANSMIT_TYPE"),false);
        appendJsonField(sb, "MX_MSGID",            asString(row, "MX_MSGID"),           false);
        appendJsonField(sb, "MX_REQUEST_TYPE",     asString(row, "MX_REQUEST_TYPE"),    false);
        appendJsonField(sb, "MX_REQUEST_SUBTYPE",  asString(row, "MX_REQUEST_SUBTYPE"), false);
        appendJsonField(sb, "MESG_STATUS",         asString(row, "MESG_STATUS"),        false);
        appendJsonField(sb, "MESG_SERVICE",        asString(row, "MESG_SERVICE"),       false);
        sb.append("}");
        return sb.toString();
    }

    private void appendJsonField(StringBuilder sb, String key, String value, boolean first) {
        if (value == null) return;
        if (sb.length() > 1) sb.append(",");
        sb.append("\"").append(key).append("\":")
          .append("\"").append(escapeJson(value)).append("\"");
    }

    // ── Type helpers ──────────────────────────────────────────────────

    private String asString(Map<String, Object> row, String column) {
        Object v = row.get(column);
        return (v != null) ? v.toString() : null;
    }

    private BigDecimal asBigDecimal(Map<String, Object> row, String column) {
        Object v = row.get(column);
        if (v == null) return null;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try { return new BigDecimal(v.toString()); } catch (Exception e) { return null; }
    }

    private LocalDateTime asLocalDateTime(Map<String, Object> row, String column) {
        Object v = row.get(column);
        if (v == null) return null;
        if (v instanceof Timestamp ts) return ts.toLocalDateTime();
        if (v instanceof LocalDateTime ldt) return ldt;
        return null;
    }

    /** Oracle CLOB 欄位需特別處理，其他字串型欄位直接 toString() */
    private String extractClob(Map<String, Object> row, String column) {
        Object v = row.get(column);
        if (v == null) return null;
        if (v instanceof Clob clob) {
            try {
                return clob.getSubString(1, (int) clob.length());
            } catch (Exception e) {
                log.warn("Failed to read CLOB column '{}': {}", column, e.getMessage());
                return null;
            }
        }
        return v.toString();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}
