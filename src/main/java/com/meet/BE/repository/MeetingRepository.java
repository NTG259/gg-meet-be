package com.meet.BE.repository;

import com.meet.BE.domain.entity.Meeting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    Optional<Meeting> findByMeetingCode(String meetingCode);

    @EntityGraph(attributePaths = "host")
    @Query(
            value = """
                    select distinct m
                    from Meeting m
                    left join MeetingParticipant mp on mp.meeting = m
                    where (m.host.id = :userId or mp.user.id = :userId or lower(mp.email) = lower(:email))
                      and (
                          (m.status = com.meet.BE.domain.enums.MeetingStatus.SCHEDULED and m.startTime >= :now)
                          or
                          (m.status = com.meet.BE.domain.enums.MeetingStatus.ACTIVE and (m.endTime is null or m.endTime >= :now))
                      )
                    order by m.startTime asc
                    """,
            countQuery = """
                    select count(distinct m)
                    from Meeting m
                    left join MeetingParticipant mp on mp.meeting = m
                    where (m.host.id = :userId or mp.user.id = :userId or lower(mp.email) = lower(:email))
                      and (
                          (m.status = com.meet.BE.domain.enums.MeetingStatus.SCHEDULED and m.startTime >= :now)
                          or
                          (m.status = com.meet.BE.domain.enums.MeetingStatus.ACTIVE and (m.endTime is null or m.endTime >= :now))
                      )
                    """
    )
    Page<Meeting> findUpcomingMeetingsForUser(
            @Param("userId") Long userId,
            @Param("email") String email,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}
