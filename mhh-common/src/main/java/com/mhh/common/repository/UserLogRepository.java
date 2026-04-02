package com.mhh.common.repository;

import com.mhh.common.entity.UserLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UserLogRepository extends JpaRepository<UserLog, Long> {

    @Modifying
    @Query("DELETE FROM UserLog l WHERE l.logTime < :threshold")
    void deleteOlderThan(LocalDateTime threshold);

    @Query("SELECT l FROM UserLog l WHERE " +
           "(:userId IS NULL OR l.userId LIKE %:userId%) AND " +
           "(:action IS NULL OR l.action = :action) AND " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:dateFrom IS NULL OR l.logTime >= :dateFrom) AND " +
           "(:dateTo IS NULL OR l.logTime <= :dateTo) " +
           "ORDER BY l.logTime DESC")
    Page<UserLog> findByFilters(
            @Param("userId") String userId,
            @Param("action") String action,
            @Param("status") String status,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable);
}
