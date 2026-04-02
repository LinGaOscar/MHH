package com.mhh.common.repository;

import com.mhh.common.entity.UserLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UserLogRepository extends JpaRepository<UserLog, Long> {

    @Modifying
    @Query("DELETE FROM UserLog l WHERE l.logTime < :threshold")
    void deleteOlderThan(LocalDateTime threshold);
}
