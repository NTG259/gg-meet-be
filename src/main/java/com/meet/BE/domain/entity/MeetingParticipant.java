package com.meet.BE.domain.entity;

import com.meet.BE.domain.enums.ParticipantStatus;
import com.meet.BE.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "meeting_participants",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"meeting_id", "user_id"}),
                @UniqueConstraint(columnNames = {"meeting_id", "guest_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long participantId;

    // ===== RELATION =====
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String email;

    // ===== GUEST =====
    @Column(name = "guest_id", length = 50)
    private String guestId;

    @Column(name = "guest_name", length = 100)
    private String guestName;

    // ===== ROLE & STATUS =====
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ParticipantStatus participantStatus;

    // ===== AUDIT =====
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @PrePersist
    @PreUpdate
    private void validate() {
        boolean hasUser = user != null;
        boolean hasGuest = guestId != null && !guestId.isBlank();
        boolean hasEmail = email != null && !email.isBlank();

        if (!hasUser && !hasGuest && !hasEmail) {
            throw new IllegalStateException("Người tham gia phải có id của tài khoản, id khách hoặc email");
        }

        if (hasUser && hasGuest) {
            throw new IllegalStateException("Người tham gia chỉ có id khách hoặc id tài khoản, không được có cả hai");
        }

        if (hasGuest && (guestName == null || guestName.isBlank())) {
            throw new IllegalStateException("Tên khách không được để trống");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeetingParticipant)) return false;
        return participantId != null && participantId.equals(((MeetingParticipant) o).getParticipantId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}