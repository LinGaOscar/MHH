package com.mhh.batch.job;

import com.mhh.common.entity.MessageHistory;
import com.mhh.common.repository.MessageHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Job to synchronize message data from external SWAL (Oracle) database.
 *
 * Flow:
 *   1. Query SWAL for unsynced rows (SYNC_STATUS = 'N'), in batches of 500.
 *   2. Skip rows already present in MSG_HISTORY (dedup by MESSAGE_ID).
 *   3. Persist new rows to MSG_HISTORY.
 *   4. Mark synced rows in SWAL as SYNC_STATUS = 'Y'.
 *
 * NOTE: SQL column names below are placeholders — update once the actual
 *       SWAL schema is confirmed (MSG_ID, MSG_TYPE, SENDER, RECEIVER, RAW_CONTENT).
 */
@Component("swalSyncJob")
@Slf4j
public class SwalSyncJob implements MhhJob {

    private static final int BATCH_SIZE = 500;

    // Placeholder SQL — replace table/column names with real SWAL schema
    private static final String QUERY_SQL =
            "SELECT MSG_ID, MSG_TYPE, SENDER, RECEIVER, RAW_CONTENT " +
            "FROM SWAL_MESSAGES WHERE SYNC_STATUS = 'N' AND ROWNUM <= " + BATCH_SIZE;

    private static final String UPDATE_STATUS_SQL =
            "UPDATE SWAL_MESSAGES SET SYNC_STATUS = 'Y', SYNC_TIME = SYSDATE " +
            "WHERE MSG_ID = ?";

    private final JdbcTemplate swalJdbcTemplate;
    private final MessageHistoryRepository messageHistoryRepository;

    public SwalSyncJob(@Qualifier("swalJdbcTemplate") JdbcTemplate swalJdbcTemplate,
                       MessageHistoryRepository messageHistoryRepository) {
        this.swalJdbcTemplate = swalJdbcTemplate;
        this.messageHistoryRepository = messageHistoryRepository;
    }

    @Override
    public String getJobName() {
        return "SwalSyncJob";
    }

    @Override
    public void execute() {
        log.info("Starting SwalSyncJob...");

        List<Map<String, Object>> rows = swalJdbcTemplate.queryForList(QUERY_SQL);
        log.info("Found {} new messages in SWAL.", rows.size());

        List<Object> synced = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            String msgId = asString(row, "MSG_ID");
            if (msgId == null) {
                log.warn("Skipping row with null MSG_ID.");
                continue;
            }

            if (messageHistoryRepository.findByMessageId(msgId).isPresent()) {
                log.debug("Message {} already exists, marking as synced.", msgId);
                synced.add(msgId);
                continue;
            }

            try {
                MessageHistory msg = MessageHistory.builder()
                        .messageId(msgId)
                        .messageType(asString(row, "MSG_TYPE"))
                        .sender(asString(row, "SENDER"))
                        .receiver(asString(row, "RECEIVER"))
                        .content(asString(row, "RAW_CONTENT"))
                        .parameters(toJson(row))
                        .source("SWAL")
                        .syncTime(LocalDateTime.now())
                        .build();

                messageHistoryRepository.save(java.util.Objects.requireNonNull(msg));
                synced.add(msgId);
                log.info("Synced message {} from SWAL.", msgId);

            } catch (Exception e) {
                log.error("Failed to sync message {}: {}", msgId, e.getMessage(), e);
                // Do NOT add to synced — will be retried next run
            }
        }

        // Mark successfully processed rows in SWAL
        if (!synced.isEmpty()) {
            for (Object id : synced) {
                swalJdbcTemplate.update(UPDATE_STATUS_SQL, id);
            }
            log.info("Marked {} messages as synced in SWAL.", synced.size());
        }

        log.info("SwalSyncJob completed. Processed: {}, Synced: {}", rows.size(), synced.size());
    }

    /**
     * Safely cast a column value to String.
     * Returns null (not the string "null") when the column is absent or SQL NULL.
     */
    private String asString(Map<String, Object> row, String column) {
        Object value = row.get(column);
        return (value != null) ? value.toString() : null;
    }

    /**
     * Serialize the raw row map to a simple JSON string for the PARAMETERS column.
     * Only includes non-null columns that are not already mapped to dedicated fields.
     */
    private String toJson(Map<String, Object> row) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String key = entry.getKey();
            // Skip columns already mapped to dedicated MSG_HISTORY fields
            if ("MSG_ID".equals(key) || "MSG_TYPE".equals(key) ||
                "SENDER".equals(key) || "RECEIVER".equals(key) || "RAW_CONTENT".equals(key)) {
                continue;
            }
            if (entry.getValue() == null) continue;

            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(key)).append("\":")
              .append("\"").append(escapeJson(entry.getValue().toString())).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}
