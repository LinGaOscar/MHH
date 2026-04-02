package com.mhh.common.repository;

import com.mhh.common.entity.MsgOutgoing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MsgOutgoingRepository extends JpaRepository<MsgOutgoing, Long> {
    Optional<MsgOutgoing> findByMessageId(String messageId);
}
