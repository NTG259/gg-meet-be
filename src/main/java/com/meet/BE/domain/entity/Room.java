package com.meet.BE.domain.entity;

import com.meet.BE.domain.status.RoomStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, name = "room_code",  length = 8)
    private String roomCode;

    @Column(name = "empty_timeout")
    private Integer emptyTimeout; // Thời gian phòng tự đóng nếu không có ai (giây)

    @Column(name = "departure_timeout")
    private Integer departureTimeout; // Thời gian chờ sau khi người cuối cùng rời đi (giây)

    @Column(name = "max_participants")
    private Integer maxParticipants; // Giới hạn số người tham gia

    @Column(columnDefinition = "TEXT")
    private String metadata; // Dữ liệu đính kèm thêm (JSON string)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status = RoomStatus.active;

}