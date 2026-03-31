package com.mhh.common.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "SYS_LOGS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LOG_ID")
    private Long logId;

    @Column(name = "LOG_LEVEL", length = 10)
    private String logLevel; // INFO, ERROR, WARN

    @Column(name = "LOGGER_NAME", length = 200)
    private String loggerName; // Class location

    @Lob
    @Column(name = "MESSAGE")
    private String message;

    @Lob
    @Column(name = "STACK_TRACE")
    private String stackTrace;

    @Column(name = "CREATE_TIME")
    private LocalDateTime createTime;

    @Column(name = "NODE_IP", length = 50)
    private String nodeIp; // Server IP for troubleshooting multi-node setup
}
