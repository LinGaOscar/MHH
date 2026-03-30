package com.mhh.batch.repository;

import com.mhh.common.entity.JobLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface JobLogRepository extends JpaRepository<JobLog, Long> {

    @Modifying
    @Query("DELETE FROM JobLog l WHERE l.startTime < :threshold")
    void deleteOlderThan(LocalDateTime threshold);
}
