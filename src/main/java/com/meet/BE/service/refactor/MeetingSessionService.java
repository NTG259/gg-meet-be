package com.meet.BE.service.refactor;

import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.enums.ParticipantStatus;
import com.meet.BE.domain.enums.Role;
import com.meet.BE.domain.request.CreateInstantMeetingRequest;
import com.meet.BE.domain.request.GuestJoinRequest;
import com.meet.BE.domain.response.MeetingResponseDTO;
import com.meet.BE.utils.convert.MeetingConvert;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingSessionService {

    private final LiveKitService liveKitService;
    private final SecurityService securityService;
    private final MeetingEntityService meetingEntityService;
    private final ParticipantServiceRF participantService;
    private final NotificationService notificationService;

    @Transactional(rollbackFor = Exception.class)
    public MeetingResponseDTO createInstantMeeting(User host, String title) {
        // Tạo phòng họp nhanh
        String meetingCode = UUID.randomUUID().toString().substring(0, 10);
        try {
            CreateInstantMeetingRequest instantMeeting = new CreateInstantMeetingRequest(title);
            liveKitService.createInstantRoom(instantMeeting);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.ROOM_CANNOT_BE_CREATED);
        }


        // Lưu thông tin vào DB
        Meeting meeting = meetingEntityService
                .createInstantMeetingEntity(host, meetingCode, title);
        MeetingParticipant hostParticipant =
                meetingEntityService.createHostParticipant(meeting, host);

        // Tạo token cho host
        String livekitToken = liveKitService.buildLiveKitToken(hostParticipant, meetingCode);

        String meetingToken = securityService.generateMeetingToken(
                hostParticipant.getParticipantId(),
                meetingCode,
                Role.HOST.name()
        );

        // Trả kết quả
        MeetingResponseDTO response = MeetingConvert.ToResponse(meeting, livekitToken);
        response.setParticipantStatus(ParticipantStatus.ACCEPT);
        response.setMeetingToken(meetingToken);

        return response;
    }

    @Transactional
    public MeetingResponseDTO joinRoomByGuest(
            GuestJoinRequest guestRequest,
            String meetingCode
    ) {
        Meeting meeting = meetingEntityService.validateMeeting(meetingCode);

        MeetingParticipant participant = participantService.handleGuestJoin(meeting, guestRequest);

        MeetingResponseDTO response = MeetingConvert.ToResponse(meeting, null);
        response.setParticipantStatus(participant.getParticipantStatus());


        String meetingToken = securityService.generateMeetingToken(
                participant.getParticipantId(),
                meetingCode,
                participant.getRole().name()
        );
        response.setMeetingToken(meetingToken);

        if (participant.getParticipantStatus() == ParticipantStatus.ACCEPT) {
            response.setLivekitToken(
                    liveKitService.buildLiveKitToken(participant, meetingCode)
            );
        } else {
            notificationService.sendJoinRequest(meetingCode, participant.getParticipantId(), participant.getGuestName());
        }
        return response;
    }

    @Transactional
    public MeetingResponseDTO joinRoomByUser(
            User currentUser,
            String meetingCode
    ) {
        // 1. Validate meeting
        Meeting meeting = meetingEntityService.validateMeeting(meetingCode);

        // 2. Handle join (user flow)
        MeetingParticipant participant =
                participantService.handleUserJoin(meeting, currentUser);

        // 3. Build base response
        MeetingResponseDTO response = MeetingConvert.ToResponse(meeting, null);
        response.setParticipantStatus(participant.getParticipantStatus());

        // 4. WebSocket token (luôn cấp)
        String meetingToken = securityService.generateMeetingToken(
                participant.getParticipantId(),
                meetingCode,
                participant.getRole().name()
        );
        response.setMeetingToken(meetingToken);

        // 5. Nếu ACCEPT → vào phòng
        if (participant.getParticipantStatus() == ParticipantStatus.ACCEPT) {
            response.setLivekitToken(
                    liveKitService.buildLiveKitToken(participant, meetingCode)
            );
        } else {
            notificationService.sendJoinRequest(
                    meetingCode,
                    participant.getParticipantId(),
                    currentUser.getFullName()
            );
        }
        return response;
    }
}
