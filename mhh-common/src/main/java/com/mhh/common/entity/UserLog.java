package com.mhh.common.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "USER_LOGS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LOG_ID")
    private Long logId;

    @Column(name = "USER_ID", length = 50)
    private String userId;

    @Column(name = "ACTION", length = 100)
    private String action; // Login, Search, Download, Appoval, etc.

    @Lob
    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "IP_ADDRESS", length = 50)
    private String ipAddress;

    @Column(name = "LOG_TIME")
    private LocalDateTime logTime;

    @Column(name = "STATUS", length = 20)
    private String status; // SUCCESS, FAILED
}
