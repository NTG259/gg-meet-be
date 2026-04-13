package com.meet.BE.service;

import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.domain.enums.ParticipantStatus;
import com.meet.BE.domain.message.MeetingMessage;
import com.meet.BE.domain.response.ParticipantsResponseDTO;
import com.meet.BE.repository.MeetingParticipantRepository;
import com.meet.BE.repository.MeetingRepository;
import com.meet.BE.repository.UserRepository;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParticipantService {
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final UserRepository userRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingService meetingService;
    public MeetingParticipant joinWaiting(MeetingMessage msg) {
        MeetingParticipant participant = new MeetingParticipant();
        participant.setMeeting(
                meetingRepository.findByMeetingCode(msg.getMeetingCode()).orElseThrow(
                        () -> new BusinessException(ErrorCode.DATA_NOT_FOUND)
                )
        );
        // Người dùng này đã đăng nhập
        if (msg.getUserId() != null) {
            participant.setUser(
                    this.userRepository.getReferenceById(msg.getUserId())
            );
        }
        return meetingParticipantRepository.save(participant);
    }

    public MeetingParticipant admit(String meetingCode, Long userId) {
        Meeting meeting = meetingService.handleFindMeeting(meetingCode);
        MeetingParticipant participant = meetingParticipantRepository.findByMeetingIdAndUserId(meeting.getId(), userId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.DATA_NOT_FOUND)
                );

        if (participant.getParticipantStatus() != ParticipantStatus.WAITING) {
            throw new RuntimeException("Trạng thái không hợp lệ");
        }

        participant.setParticipantStatus(ParticipantStatus.ACCEPT);
        return this.meetingParticipantRepository.save(participant);
    }

    public MeetingParticipant reject(String meetingCode, Long userId) {
        Meeting meeting = meetingService.handleFindMeeting(meetingCode);
        MeetingParticipant participant= meetingParticipantRepository.findByMeetingIdAndUserId(meeting.getId(), userId)
                .orElseThrow();

        participant.setParticipantStatus(ParticipantStatus.REJECT);
        return meetingParticipantRepository.save(participant);
    }
}
