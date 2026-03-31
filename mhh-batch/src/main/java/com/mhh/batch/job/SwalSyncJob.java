package com.mhh.batch.job;

import com.mhh.common.entity.MessageHistory;
import com.mhh.common.repository.MessageHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Job to synchronize message data from external SWAL (Oracle) database.
 */
@Component("swalSyncJob")
@Slf4j
public class SwalSyncJob implements MhhJob {
    
    @Override
    public String getJobName() {
        return "SwalSyncJob";
    }

    private final JdbcTemplate swalJdbcTemplate;
    private final MessageHistoryRepository messageHistoryRepository;

    @Autowired
    public SwalSyncJob(@Qualifier("swalJdbcTemplate") JdbcTemplate swalJdbcTemplate,
                       MessageHistoryRepository messageHistoryRepository) {
        this.swalJdbcTemplate = swalJdbcTemplate;
        this.messageHistoryRepository = messageHistoryRepository;
    }

    public void execute() {
        log.info("Starting SwalSyncJob...");
        
        try {
            // Placeholder query - to be updated once SWAL schema is finalized
            String sql = "SELECT * FROM SWAL_MESSAGES WHERE SYNC_STATUS = 'N'";
            
            List<Map<String, Object>> rows = swalJdbcTemplate.queryForList(sql);
            log.info("Found {} new messages in SWAL.", rows.size());

            for (Map<String, Object> row : rows) {
                processRow(row);
            }
            
            log.info("SwalSyncJob completed successfully.");
        } catch (Exception e) {
            log.error("Error during SwalSyncJob execution: {}", e.getMessage(), e);
        }
    }

    private void processRow(Map<String, Object> row) {
        String msgId = String.valueOf(row.get("MSG_ID"));
        
        if (messageHistoryRepository.findByMessageId(msgId).isPresent()) {
            log.debug("Message {} already exists, skipping.", msgId);
            return;
        }

        MessageHistory msg = MessageHistory.builder()
                .messageId(msgId)
                .messageType(String.valueOf(row.get("MSG_TYPE")))
                .sender(String.valueOf(row.get("SENDER")))
                .receiver(String.valueOf(row.get("RECEIVER")))
                .content(String.valueOf(row.get("RAW_CONTENT")))
                .source("SWAL")
                .syncTime(LocalDateTime.now())
                .build();
        
        // Flexible: Store everything else in PARAMETERS as simple string representation for now
        msg.setParameters(row.toString()); 
        
        messageHistoryRepository.save(msg);
        log.info("Synced message {} from SWAL.", msgId);
    }
}
