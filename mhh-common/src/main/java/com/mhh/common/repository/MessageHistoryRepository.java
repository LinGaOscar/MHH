package com.mhh.common.repository;

import com.mhh.common.entity.MessageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MessageHistoryRepository extends JpaRepository<MessageHistory, Long> {
    Optional<MessageHistory> findByMessageId(String messageId);
}
