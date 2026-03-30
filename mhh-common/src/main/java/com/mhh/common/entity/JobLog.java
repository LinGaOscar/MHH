package com.mhh.common.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "JOBS_LOGS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RUN_ID")
    private Long runId;

    @Column(name = "JOB_NAME", length = 100)
    private String jobName;

    @Column(name = "START_TIME")
    private LocalDateTime startTime;

    @Column(name = "END_TIME")
    private LocalDateTime endTime;

    @Column(name = "STATUS", length = 20)
    private String status;

    @Column(name = "ERROR_MSG", columnDefinition = "NVARCHAR(MAX)")
    private String errorMsg;
}
