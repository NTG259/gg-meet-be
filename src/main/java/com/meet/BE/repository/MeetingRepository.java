package com.meet.BE.repository;

import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.enums.MeetingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    Optional<Meeting> findByMeetingCode(String meetingCode);

    long countByStatus(MeetingStatus status);

    long countByStartTimeGreaterThanEqualAndStartTimeLessThan(
            LocalDateTime from,
            LocalDateTime to
    );

    long countByStatusAndStartTimeGreaterThanEqualAndStartTimeLessThan(
            MeetingStatus status,
            LocalDateTime from,
            LocalDateTime to
    );

    long countByHostId(Long hostId);

    @Query("""
            select m.host.id as hostId, count(m) as meetingCount
            from Meeting m
            where m.host.id in :hostIds
            group by m.host.id
            """)
    List<HostMeetingCount> countMeetingsByHostIds(@Param("hostIds") List<Long> hostIds);

    interface HostMeetingCount {
        Long getHostId();

        Long getMeetingCount();
    }

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
                      and m.isScheduled = true
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

    @EntityGraph(attributePaths = "host")
    @Query(
            value = """
                    select m
                    from Meeting m
                    left join m.host h
                    where (:status is null or m.status = :status)
                      and (
                          :search is null
                          or lower(m.title) like lower(concat('%', :search, '%'))
                          or lower(m.meetingCode) like lower(concat('%', :search, '%'))
                          or lower(h.fullName) like lower(concat('%', :search, '%'))
                          or lower(h.email) like lower(concat('%', :search, '%'))
                      )
                      and (:fromTime is null or m.startTime >= :fromTime)
                      and (:toTime is null or m.startTime <= :toTime)
                    """,
            countQuery = """
                    select count(m)
                    from Meeting m
                    left join m.host h
                    where (:status is null or m.status = :status)
                      and (
                          :search is null
                          or lower(m.title) like lower(concat('%', :search, '%'))
                          or lower(m.meetingCode) like lower(concat('%', :search, '%'))
                          or lower(h.fullName) like lower(concat('%', :search, '%'))
                          or lower(h.email) like lower(concat('%', :search, '%'))
                      )
                      and (:fromTime is null or m.startTime >= :fromTime)
                      and (:toTime is null or m.startTime <= :toTime)
                    """
    )
    Page<Meeting> findForAdmin(
            @Param("status") MeetingStatus status,
            @Param("search") String search,
            @Param("fromTime") LocalDateTime from,
            @Param("toTime") LocalDateTime to,
            Pageable pageable
    );
}
