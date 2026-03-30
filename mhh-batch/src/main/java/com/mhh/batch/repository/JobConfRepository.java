package com.mhh.batch.repository;

import com.mhh.common.entity.JobConf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobConfRepository extends JpaRepository<JobConf, String> {
    List<JobConf> findByIsEnabledTrue();

    @Modifying
    @Query("UPDATE JobConf j SET j.lastRun = :lastRun WHERE j.jobName = :jobName")
    void updateLastRun(String jobName, LocalDateTime lastRun);
}
