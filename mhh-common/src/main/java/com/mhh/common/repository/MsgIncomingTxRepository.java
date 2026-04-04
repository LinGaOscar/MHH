package com.mhh.common.repository;

import com.mhh.common.entity.MsgIncomingTx;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MsgIncomingTxRepository extends JpaRepository<MsgIncomingTx, Long> {
    Optional<MsgIncomingTx> findByMessageId(String messageId);
}
