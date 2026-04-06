package com.meet.BE.domain.entity;

import com.meet.BE.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "guest_name", length = 100)
    private String guestName;

    @Enumerated(EnumType.STRING)
    private Role role;

    // Ghi đè equals và hashCode chỉ dựa trên ID để tối ưu cho Set/Map của Hibernate
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeetingParticipant)) return false;
        return id != null && id.equals(((MeetingParticipant) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}