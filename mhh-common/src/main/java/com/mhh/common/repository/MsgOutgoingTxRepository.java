package com.mhh.common.repository;

import com.mhh.common.entity.MsgOutgoingTx;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MsgOutgoingTxRepository extends JpaRepository<MsgOutgoingTx, Long> {
    Optional<MsgOutgoingTx> findByMessageId(String messageId);
}
