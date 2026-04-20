package com.meet.BE.service.refactor;

import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.enums.MeetingStatus;
import com.meet.BE.domain.enums.ParticipantStatus;
import com.meet.BE.domain.enums.Role;
import com.meet.BE.domain.request.CreateInstantMeetingRequest;
import com.meet.BE.domain.request.GuestJoinRequest;
import com.meet.BE.domain.request.ScheduleMeetingRequest;
import com.meet.BE.domain.response.MeetingResponseDTO;
import com.meet.BE.utils.convert.MeetingConvert;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import com.meet.BE.utils.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingOrchestratorService {

    private final LiveKitService liveKitService;
    private final SecurityService securityService;
    private final MeetingEntityService meetingEntityService;
    private final ParticipantServiceRF participantService;
    private final NotificationService notificationService;
    private final ParticipantPermissionService permissionService;
    private final ParticipantEntityService participantEntityService;
    private final EmailService emailService;
    private final ScheduleService scheduleService;

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
        Meeting meeting = meetingEntityService.validateMeetingBeforeJoin(meetingCode);

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
        Meeting meeting = meetingEntityService.validateMeetingBeforeJoin(meetingCode);

        MeetingParticipant participant =
                participantService.handleInvitedJoin(
                        meeting,
                        currentUser.getEmail(),
                        currentUser,
                        null
                );

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
            notificationService.sendJoinRequest(
                    meetingCode,
                    participant.getParticipantId(),
                    currentUser.getFullName()
            );
        }
        return response;
    }


    @Transactional
    public void cancelJoin(
            Long targetParticipantId,
            String meetingCode) {
        MeetingParticipant removeParticipant = participantService.cancelWaiting(
                targetParticipantId,
                meetingCode
        );

        if (removeParticipant == null) return;
        String removeParticipantName = null;
        if (removeParticipant.getGuestName() == null) {
            removeParticipantName = removeParticipant.getUser().getFullName();
        } else {
            removeParticipantName = removeParticipant.getGuestName();
        }

        notificationService.sendWaitingCancelled(
                meetingCode,
                removeParticipant.getParticipantId(),
                removeParticipantName
        );
    }


    @Transactional
    public MeetingParticipant admit(String meetingCode, Long targetParticipantId, Long hostId) {

        MeetingParticipant host = this.participantEntityService.getParticipantInMeeting(hostId, meetingCode);
        permissionService.assertHostPermission(host);
        MeetingParticipant participant = this.participantEntityService.validateParticipantInMeeting(targetParticipantId, meetingCode);
        participant = this.participantService.handleAcceptParticipant(participant);
        String name = participant.getUser() == null ? participant.getGuestName() : participant.getUser().getFullName();
        notificationService.sendAdmit(meetingCode, participant.getParticipantId(), name);

        return participant;
    }


    @Transactional
    public void reject(String meetingCode, Long targetParticipantId, Long hostId) {
        MeetingParticipant host = this.participantEntityService.getParticipantInMeeting(hostId, meetingCode);
        permissionService.assertHostPermission(host);

        MeetingParticipant participant = this.participantEntityService.validateParticipantInMeeting(targetParticipantId, meetingCode);

        MeetingParticipant newParticipant = this.participantService.handleRejectParticipant(participant);
        String participantName = newParticipant.getGuestName() == null ?
                newParticipant.getUser().getFullName() : newParticipant.getGuestName();

        notificationService.sendReject(meetingCode, targetParticipantId, hostId ,participantName);
    }

    @Transactional
    public void endMeetingForEverybody(User currentUser, String meetingCode) {
        Meeting meeting = this.meetingEntityService.validateMeetingBeforeDelete(meetingCode);

        MeetingParticipant host = this.participantEntityService.getUserInMeeting(currentUser.getId(), meetingCode);
        this.permissionService.assertHostPermission(host);

        this.liveKitService.deleteRoom(meetingCode);

        this.meetingEntityService.updateMeetingStatus(meeting, MeetingStatus.ENDED);
        // 2. sau đó mới gửi WS
        notificationService.sendMeetingEnded(meetingCode);
    }

    @Transactional
    public void leaveMeeting(Long participantId, String meetingCode) {
        MeetingParticipant participant = this.participantEntityService.getParticipantInMeeting(participantId, meetingCode);

        this.participantEntityService.updateParticipantStatus(participant, ParticipantStatus.LEFT);

        notificationService.sendParticipantLeft(meetingCode, participantId);
    }

    @Transactional
    public MeetingResponseDTO createScheduleMeeting(User host, ScheduleMeetingRequest request) {

        Meeting scheduleMeeting = this.meetingEntityService.createScheduleMeetingEntity(host, request);

        this.scheduleService.pushToSchedule(
                scheduleMeeting.getId(),
                scheduleMeeting.getStartTime()
                        .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                        .toEpochSecond());

        this.participantEntityService.saveInvitedUser(scheduleMeeting, request.getEmailList());

        for (String email : request.getEmailList()) {
            this.emailService.sendMeetingInvitation(
                    email,
                    host.getFullName(),
                    scheduleMeeting.getTitle(),
                    String.valueOf(scheduleMeeting.getStartTime()),
                    scheduleMeeting.getMeetingCode()
            );
        }

        MeetingResponseDTO response = MeetingConvert.ToResponse(scheduleMeeting, null);
        response.setParticipantStatus(ParticipantStatus.INVITED);
        return response;
    }

    @Transactional
    public MeetingResponseDTO activeMeeting(Long meetingId) {
        Meeting meeting = this.meetingEntityService.findByMeetingId(meetingId);

        if (meeting.getStatus() == MeetingStatus.ACTIVE) {
            return MeetingConvert.ToResponse(meeting, null);
        }

        Meeting activedMeeting = this.meetingEntityService.updateMeetingStatus(meeting, MeetingStatus.ACTIVE);

        return MeetingConvert.ToResponse(activedMeeting, null);
    }
}
