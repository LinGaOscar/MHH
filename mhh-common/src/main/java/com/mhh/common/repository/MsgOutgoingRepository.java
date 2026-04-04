package com.mhh.common.repository;

import com.mhh.common.entity.MsgOutgoing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MsgOutgoingRepository extends JpaRepository<MsgOutgoing, Long> {
    Optional<MsgOutgoing> findByMessageId(String messageId);

    /** 多條件搜尋（僅查 MSG_OUTGOING 搜尋表，不載入電文內容） */
    @Query("SELECT m FROM MsgOutgoing m WHERE " +
           "(:dateFrom IS NULL OR m.msgDate >= :dateFrom) AND " +
           "(:dateTo IS NULL OR m.msgDate <= :dateTo) AND " +
           "(:msgTypePrefix IS NULL OR m.messageType LIKE CONCAT(:msgTypePrefix, '%')) AND " +
           "(:sender IS NULL OR m.sender LIKE CONCAT('%', :sender, '%')) AND " +
           "(:receiver IS NULL OR m.receiver LIKE CONCAT('%', :receiver, '%')) AND " +
           "(:amountFrom IS NULL OR m.amount >= :amountFrom) AND " +
           "(:amountTo IS NULL OR m.amount <= :amountTo) AND " +
           "(:reference IS NULL OR m.reference LIKE CONCAT('%', :reference, '%')) AND " +
           "(:tag20 IS NULL OR m.tag20 LIKE CONCAT('%', :tag20, '%')) AND " +
           "(:tag21 IS NULL OR m.tag21 LIKE CONCAT('%', :tag21, '%')) AND " +
           "(:osnFrom IS NULL OR m.osn >= :osnFrom) AND " +
           "(:osnTo IS NULL OR m.osn <= :osnTo) AND " +
           "(:unitCode IS NULL OR m.unitCode = :unitCode) AND " +
           "(:amlFlag IS NULL OR m.amlFlag = :amlFlag) AND " +
           "(:amlStatus IS NULL OR m.amlStatus = :amlStatus) " +
           "ORDER BY m.msgDate DESC")
    Page<MsgOutgoing> findByFilters(
            @Param("dateFrom")      LocalDateTime dateFrom,
            @Param("dateTo")        LocalDateTime dateTo,
            @Param("msgTypePrefix") String msgTypePrefix,
            @Param("sender")        String sender,
            @Param("receiver")      String receiver,
            @Param("amountFrom")    BigDecimal amountFrom,
            @Param("amountTo")      BigDecimal amountTo,
            @Param("reference")     String reference,
            @Param("tag20")         String tag20,
            @Param("tag21")         String tag21,
            @Param("osnFrom")       String osnFrom,
            @Param("osnTo")         String osnTo,
            @Param("unitCode")      String unitCode,
            @Param("amlFlag")       String amlFlag,
            @Param("amlStatus")     String amlStatus,
            Pageable pageable);
}
