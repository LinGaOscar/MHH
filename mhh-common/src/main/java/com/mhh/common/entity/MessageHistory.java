package com.mhh.common.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "MSG_HISTORY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "MESSAGE_ID", unique = true)
    private String messageId;

    @Column(name = "MESSAGE_TYPE", length = 50)
    private String messageType;

    @Column(name = "SENDER", length = 50)
    private String sender;

    @Column(name = "RECEIVER", length = 50)
    private String receiver;

    @Column(name = "AMOUNT", precision = 18, scale = 5)
    private BigDecimal amount;

    @Column(name = "CURRENCY", length = 3)
    private String currency;

    @Column(name = "REFERENCE", length = 100)
    private String reference;

    @Lob
    @Column(name = "CONTENT")
    private String content;

    @Lob
    @Column(name = "PARAMETERS")
    private String parameters; // Used for flexible/extra fields in JSON format

    @Column(name = "SYNC_TIME")
    private LocalDateTime syncTime;

    @Column(name = "SOURCE", length = 20)
    private String source; // e.g., "SWAL", "PDF"
}
