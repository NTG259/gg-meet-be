//package com.meet.BE.service;
//
//import com.meet.BE.domain.entity.MeetingParticipant;
//import com.meet.BE.domain.enums.ParticipantStatus;
//import com.meet.BE.domain.enums.Role;
//import com.meet.BE.domain.message.MeetingWsMessage;
//import com.meet.BE.domain.request.GuestJoinRequest;
//import com.meet.BE.domain.entity.Meeting;
//import com.meet.BE.domain.entity.User;
//import com.meet.BE.domain.response.MeetingResponseDTO;
//import com.meet.BE.domain.enums.MeetingStatus;
//import com.meet.BE.repository.MeetingParticipantRepository;
//import com.meet.BE.repository.MeetingRepository;
//import com.meet.BE.service.refactor.SecurityService;
//import com.meet.BE.utils.convert.MeetingConvert;
//import com.meet.BE.utils.errors.BusinessException;
//import com.meet.BE.utils.errors.ErrorCode;
//import io.livekit.server.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import tools.jackson.databind.ObjectMapper;
//
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class InstantMeetingService {
//
//    private final MeetingParticipantRepository meetingParticipantRepository;
//    private final MeetingRepository meetingRepository;
//    private final RoomServiceClient roomServiceClient;
//    private final ParticipantService participantService;
//    private final SecurityService securityService;
//    private final SimpMessagingTemplate messagingTemplate;
//
//    @Value("${livekit.api-key}")
//    private String apiKey;
//
//    @Value("${livekit.api-secret}")
//    private String apiSecret;
//
//    @Transactional(rollbackFor = Exception.class)
//    public void endMeeting(User currentUser, String meetingCode) throws Exception {
//        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode)
//                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
//
//        if (meeting.getStatus() != MeetingStatus.ACTIVE) {
//            throw new BusinessException(ErrorCode.MEETING_ALREADY_ENDED);
//        }
//
//        if (!meeting.getHost().getId().equals(currentUser.getId())) {
//            throw new BusinessException(ErrorCode.NOT_PERMIT);
//        }
//
//        // 1. Xoá phòng LiveKit
//        try {
//            roomServiceClient.deleteRoom(meetingCode).execute();
//        } catch (Exception e) {
//            // Log lỗi thay vì System.err
//            log.error("LiveKit Room delete error: {}", e.getMessage());
//        }
//
//        // 2. Cập nhật DB
//        meeting.setStatus(MeetingStatus.ENDED);
//        meetingRepository.save(meeting);
//
//        // 3. BẮN THÔNG BÁO WEBSOCKET CHO TẤT CẢ MỌI NGƯỜI
//        MeetingWsMessage socketMessage = new MeetingWsMessage();
//        socketMessage.setMeetingCode(meetingCode);
//        socketMessage.setAction("MEETING_ENDED");
//        messagingTemplate.convertAndSend("/topic/meeting/" + meetingCode, socketMessage);
//    }
//
//    @Transactional(rollbackFor = Exception.class)
//    public void leaveMeeting(Long participantId, String meetingCode) {
//        // 1. Tìm bản ghi Participant trong DB
//        MeetingParticipant participant = meetingParticipantRepository.findById(participantId)
//                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));
//
//        // Kiểm tra xem có đúng là đang ở trong phòng này không
//        if (!participant.getMeeting().getMeetingCode().equals(meetingCode)) {
//            throw new BusinessException(ErrorCode.USER_NOT_IN_THIS_ROOM);
//        }
//
//        // 2. Cập nhật trạng thái của người này thành LEFT
//        participant.setParticipantStatus(ParticipantStatus.LEFT);
//        meetingParticipantRepository.save(participant);
//
//        // 3. BẮN THÔNG BÁO CHO NHỮNG NGƯỜI CÒN LẠI
//        MeetingWsMessage socketMessage = new MeetingWsMessage();
//        socketMessage.setMeetingCode(meetingCode);
//        socketMessage.setTargetParticipantId(participantId);
//        socketMessage.setAction("PARTICIPANT_LEFT");
//
//        // Broadcast vào phòng
//        messagingTemplate.convertAndSend("/topic/meeting/" + meetingCode, socketMessage);
//    }
//
//    public String checkValidMeetingCode(String meetingCode) {
//        Meeting meeting = this.meetingRepository.findByMeetingCode(meetingCode).orElseThrow(
//                () -> new BusinessException(ErrorCode.ROOM_NOT_FOUND)
//        );
//
//        if (meeting.getEndTime() != null && meeting.getEndTime().isBefore(LocalDateTime.now())
//                || meeting.getStatus() != MeetingStatus.ACTIVE) {
//            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND);
//        }
//        return meetingCode;
//    }
//}