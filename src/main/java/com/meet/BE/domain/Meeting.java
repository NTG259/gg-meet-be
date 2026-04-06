package com.meet.BE.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "meetings",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "meeting_code")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "meeting_code", nullable = false, unique = true, length = 50)
    private String meetingCode;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Column(name = "is_scheduled", nullable = false)
    private Boolean isScheduled;

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    @Column(name = "status", nullable = false, length = 20)
    private String status;
}