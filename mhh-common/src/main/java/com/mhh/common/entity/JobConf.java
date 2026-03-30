package com.mhh.common.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "JOBS_CONF")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobConf {

    @Id
    @Column(name = "JOB_NAME", length = 100)
    private String jobName;

    @Column(name = "DESCRIPTION", length = 200)
    private String description;

    @Column(name = "CRON_SEC", length = 10)
    private String cronSec;

    @Column(name = "CRON_MIN", length = 10)
    private String cronMin;

    @Column(name = "CRON_HOUR", length = 10)
    private String cronHour;

    @Column(name = "CRON_DOM", length = 10)
    private String cronDom;

    @Column(name = "CRON_MONTH", length = 10)
    private String cronMonth;

    @Column(name = "CRON_DOW", length = 10)
    private String cronDow;

    @Column(name = "IS_ENABLED")
    private Boolean isEnabled;

    @Column(name = "LAST_RUN")
    private LocalDateTime lastRun;

    @Column(name = "PARAMS", columnDefinition = "NVARCHAR(MAX)")
    private String params;

    /**
     * 動態組合 6 位標準 Spring Cron 表達式
     * 格式: 秒 分 時 日 月 週
     */
    public String getFullCronExpression() {
        return String.format("%s %s %s %s %s %s",
                clean(cronSec), clean(cronMin), clean(cronHour),
                clean(cronDom), clean(cronMonth), clean(cronDow));
    }

    private String clean(String val) {
        return (val == null || val.trim().isEmpty()) ? "*" : val.trim();
    }
}
