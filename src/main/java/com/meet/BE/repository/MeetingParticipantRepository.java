package com.meet.BE.repository;

import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.domain.enums.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {
    Optional<MeetingParticipant> findByMeetingIdAndUserId(Long meetingId, Long userId);
    Optional<MeetingParticipant> findByMeetingIdAndGuestId(Long id, String guestId);
    Optional<MeetingParticipant> findByMeetingIdAndEmail(Long meetingId, String email);
    Optional<MeetingParticipant> findByMeetingIdAndEmailIgnoreCase(Long meetingId, String email);
    List<MeetingParticipant> findAllByMeetingIdAndParticipantStatusOrderByCreatedAtAsc(
            Long meetingId,
            ParticipantStatus participantStatus
    );

    @Query("""
            select mp.meeting.id as meetingId, count(mp) as participantCount
            from MeetingParticipant mp
            where mp.meeting.id in :meetingIds
              and mp.participantStatus = com.meet.BE.domain.enums.ParticipantStatus.ACCEPT
            group by mp.meeting.id
            """)
    List<AcceptedParticipantCount> countAcceptedParticipantsByMeetingIds(@Param("meetingIds") List<Long> meetingIds);

    interface AcceptedParticipantCount {
        Long getMeetingId();

        Long getParticipantCount();
    }
}
