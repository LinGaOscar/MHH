package com.mhh.common.repository;

import com.mhh.common.entity.MsgIncoming;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MsgIncomingRepository extends JpaRepository<MsgIncoming, Long> {
    Optional<MsgIncoming> findByMessageId(String messageId);
}
