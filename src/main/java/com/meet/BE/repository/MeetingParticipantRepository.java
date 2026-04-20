package com.meet.BE.repository;

import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.enums.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {
    Optional<MeetingParticipant> findByMeetingIdAndUserId(Long meetingId, Long userId);
    Optional<MeetingParticipant> findByMeetingIdAndParticipantId(Long meetingId, Long participantId);
    Optional<MeetingParticipant> findByMeetingIdAndGuestId(Long id, String guestId);
    Optional<MeetingParticipant> findByMeetingIdAndEmail(Long meetingId, String email);
    Optional<MeetingParticipant> findByMeetingIdAndEmailIgnoreCase(Long meetingId, String email);
    List<MeetingParticipant> findAllByMeetingIdAndParticipantStatusOrderByCreatedAtAsc(
            Long meetingId,
            ParticipantStatus participantStatus
    );
}
