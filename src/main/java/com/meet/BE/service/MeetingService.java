package com.meet.BE.service;

import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.domain.enums.ParticipantStatus;
import com.meet.BE.domain.enums.Role;
import com.meet.BE.domain.message.MeetingWsMessage;
import com.meet.BE.domain.request.CreateMeetingRequest;
import com.meet.BE.domain.request.GuestJoinRequest;
import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.response.MeetingResponseDTO;
import com.meet.BE.domain.enums.MeetingStatus;
import com.meet.BE.repository.MeetingParticipantRepository;
import com.meet.BE.repository.MeetingRepository;
import com.meet.BE.utils.convert.MeetingConvert;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import io.livekit.server.*;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingRepository meetingRepository;
    private final RoomServiceClient roomServiceClient;
    private final ParticipantService participantService;
    private final SecurityService securityService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${livekit.api-key}")
    private String apiKey;

    @Value("${livekit.api-secret}")
    private String apiSecret;

    @Transactional(rollbackFor = Exception.class)
    public MeetingResponseDTO createMeeting(User host, String title) throws Exception {
        CreateMeetingRequest createMeetingRequest = new CreateMeetingRequest(title);
        String meetingCode = UUID.randomUUID().toString().substring(0, 10);
        livekit.LivekitRoom.CreateRoomRequest livekitRequest = livekit.LivekitRoom.CreateRoomRequest.newBuilder()
                .setName(meetingCode)
                .setEmptyTimeout(createMeetingRequest.getEmptyTimeout())
                .setDepartureTimeout(createMeetingRequest.getDepartureTimeout())
                .setMaxParticipants(createMeetingRequest.getMaxParticipants())
                .build();

        roomServiceClient.createRoom(
                livekitRequest.getName(),
                livekitRequest.getEmptyTimeout(),
                livekitRequest.getMaxParticipants(),
                "",
                livekitRequest.getMetadata()
        ).execute();

        Meeting meeting = new Meeting();
        meeting.setHost(host);
        meeting.setMeetingCode(meetingCode);
        meeting.setTitle(createMeetingRequest.getTitle());
        meeting.setStatus(MeetingStatus.ACTIVE);
        meeting.setCreatedAt(LocalDateTime.now());
        meeting.setStartTime(LocalDateTime.now());
        meeting.setRoomSid(meetingCode);
        Meeting savedMeeting = meetingRepository.save(meeting);

        // [ĐÃ FIX LỖI]: Phải lưu cả Host vào bảng MeetingParticipant
        MeetingParticipant hostParticipant = new MeetingParticipant();
        hostParticipant.setMeeting(savedMeeting);
        hostParticipant.setUser(host);
        hostParticipant.setRole(Role.HOST);
        hostParticipant.setParticipantStatus(ParticipantStatus.ACCEPT);
        hostParticipant = meetingParticipantRepository.save(hostParticipant);

        // Tạo Livekit Token cho Host
        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setName(host.getFullName());
        token.setIdentity(host.getId().toString());
        token.addGrants(
                new VideoGrant[]{
                        new RoomJoin(true),
                        new RoomName(meetingCode),
                        new CanPublish(true),
                        new CanSubscribe(true),
                        new CanUpdateOwnMetadata(true),
                }
        );
        String livekitToken = token.toJwt();

        MeetingResponseDTO response = MeetingConvert.ToResponse(savedMeeting, livekitToken);
        response.setParticipantStatus(ParticipantStatus.ACCEPT);
        response.setTitle(meeting.getTitle());
        // Tạo vé WebSocket Token (JWT Xịn)
        String meetingToken = securityService.generateMeetingToken(
                hostParticipant.getParticipantId(),
                meetingCode,
                Role.HOST.name() // Host tạo phòng nên mặc định truyền role HOST
        );
        response.setMeetingToken(meetingToken);

        return response;
    }

    /**
     * Tham gia vào một cuộc họp đã tồn tại (Hỗ trợ cả User & Guest)
     */
    @Transactional
    public MeetingResponseDTO processJoinMeeting(String email, String meetingCode, GuestJoinRequest guestRequest) {
        Meeting meeting = handleFindMeeting(meetingCode);

        if (meeting.getStatus() != MeetingStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND);
        }

        // 1. Gọi ParticipantService xử lý phân quyền User/Guest, lưu DB, và tự động bắn WS cho Host
        MeetingParticipant participant = participantService.handleParticipantJoin(email, meetingCode, guestRequest);

        // 2. Map data cơ bản ra DTO
        MeetingResponseDTO response = MeetingConvert.ToResponse(meeting, null);
        response.setParticipantStatus(participant.getParticipantStatus());

        // 3. Cấp Vé WebSocket (Dùng để Frontend connect STOMP)
        String meetingToken = securityService.generateMeetingToken(
                participant.getParticipantId(),
                meetingCode,
                participant.getRole().name()
        );
        response.setMeetingToken(meetingToken);

        // 4. CHỈ cấp LiveKit Token nếu người này là Host hoặc đã được ACCEPT
        // (Nếu đang WAITING, token LiveKit sẽ bị null -> bảo mật tốt hơn, không cho khách lén vào phòng)
        if (participant.getParticipantStatus() == ParticipantStatus.ACCEPT) {
            AccessToken token = new AccessToken(apiKey, apiSecret);

            // Xử lý tên và ID tuỳ thuộc vào User hay Guest
            String displayName = participant.getUser() != null ?
                    participant.getUser().getFullName() : participant.getGuestName();
            String identity = participant.getUser() != null ?
                    participant.getUser().getId().toString() : "guest-" + participant.getGuestId();

            token.setName(displayName);
            token.setIdentity(identity);
            token.addGrants(
                    new VideoGrant[]{
                            new RoomJoin(true),
                            new RoomName(meetingCode),
                            new CanPublish(true),
                            new CanSubscribe(true),
                            new CanUpdateOwnMetadata(true),
                    }
            );
            // Set role cho User tham gia Host : Participant
            String roleName = participant.getRole() != null ? participant.getRole().name() : Role.PARTICIPANT.toString();
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> metadataMap = new HashMap<>();
            metadataMap.put("role", roleName);
            // Chuyển Map thành chuỗi JSON an toàn
            String metadataJson = objectMapper.writeValueAsString(metadataMap);

            token.setMetadata(metadataJson);

            response.setLivekitToken(token.toJwt());
        }

        return response;
    }

//    @Transactional
//    public MeetingResponseDTO processJoinMeeting(String email, String meetingCode, GuestJoinRequest guestRequest) {
//        Meeting meeting = handleFindMeeting(meetingCode);
//
//        if (meeting.getStatus() != MeetingStatus.ACTIVE) {
//            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND);
//        }
//
//        // 1. Gọi ParticipantService xử lý tham gia, lưu DB
//        MeetingParticipant participant = participantService.handleParticipantJoin(email, meetingCode, guestRequest);
//
//        // 2. Map data cơ bản ra DTO
//        MeetingResponseDTO response = MeetingConvert.ToResponse(meeting, null);
//        response.setParticipantStatus(participant.getParticipantStatus());
//
//        // 3. Cấp Vé WebSocket (Dùng để Frontend connect STOMP)
//        String meetingToken = securityService.generateMeetingToken(
//                participant.getParticipantId(),
//                meetingCode,
//                participant.getRole().name()
//        );
//        response.setMeetingToken(meetingToken);
//
//        // 4. CẤP THẲNG LIVEKIT TOKEN (Không cần check phòng chờ nữa)
//        AccessToken token = new AccessToken(apiKey, apiSecret);
//
//        // Xử lý tên và ID tuỳ thuộc vào User hay Guest
//        String displayName = participant.getUser() != null ?
//                participant.getUser().getFullName() : participant.getGuestName();
//        String identity = participant.getUser() != null ?
//                participant.getUser().getId().toString() : "guest-" + participant.getGuestId();
//
//        token.setName(displayName);
//        token.setIdentity(identity);
//        token.addGrants(new RoomJoin(true));
//        token.addGrants(new RoomName(meetingCode));
//
//        // Set role cho User tham gia Host : Participant
//        String roleName = participant.getRole() != null ? participant.getRole().name() : Role.PARTICIPANT.toString();
//        ObjectMapper objectMapper = new ObjectMapper();
//        Map<String, Object> metadataMap = new HashMap<>();
//        metadataMap.put("role", roleName);
//        // Chuyển Map thành chuỗi JSON an toàn
//        String metadataJson = objectMapper.writeValueAsString(metadataMap);
//
//        token.setMetadata(metadataJson);
//        response.setLivekitToken(token.toJwt());
//
//        return response;
//    }

    private Meeting handleFindMeeting(String meetingCode) {
        return this.meetingRepository.findByMeetingCode(meetingCode).orElseThrow(
                () -> new BusinessException(ErrorCode.ROOM_NOT_FOUND)
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void endMeeting(User currentUser, String meetingCode) throws Exception {
        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        if (meeting.getStatus() != MeetingStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.MEETING_ALREADY_ENDED);
        }

        if (!meeting.getHost().getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.NOT_PERMIT);
        }

        // 1. Xoá phòng LiveKit
        try {
            roomServiceClient.deleteRoom(meetingCode).execute();
        } catch (Exception e) {
            // Log lỗi thay vì System.err
            System.err.println("LiveKit Room delete error: " + e.getMessage());
        }

        // 2. Cập nhật DB
        meeting.setStatus(MeetingStatus.ENDED);
        meetingRepository.save(meeting);

        // 3. BẮN THÔNG BÁO WEBSOCKET CHO TẤT CẢ MỌI NGƯỜI
        MeetingWsMessage socketMessage = new MeetingWsMessage();
        socketMessage.setMeetingCode(meetingCode);
        socketMessage.setAction("MEETING_ENDED"); // Frontend sẽ check action này
        // Đối với event End Meeting, bạn không cần set targetParticipantId vì gửi cho tất cả
        // Gửi vào topic chung của meeting, ví dụ: /topic/meeting/123456
        messagingTemplate.convertAndSend("/topic/meeting/" + meetingCode, socketMessage);
    }

    @Transactional(rollbackFor = Exception.class)
    public void leaveMeeting(Long participantId, String meetingCode) {
        // 1. Tìm bản ghi Participant trong DB
        MeetingParticipant participant = meetingParticipantRepository.findById(participantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));

        // Kiểm tra xem có đúng là đang ở trong phòng này không
        if (!participant.getMeeting().getMeetingCode().equals(meetingCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_IN_THIS_ROOM);
        }

        // 2. Cập nhật trạng thái của người này thành LEFT (Đã rời đi)
        // Lưu ý: Nhớ thêm LEFT vào file Enum ParticipantStatus nhé
        participant.setParticipantStatus(ParticipantStatus.LEFT);
        meetingParticipantRepository.save(participant);

        // 3. BẮN THÔNG BÁO CHO NHỮNG NGƯỜI CÒN LẠI
        MeetingWsMessage socketMessage = new MeetingWsMessage();
        socketMessage.setMeetingCode(meetingCode);
        socketMessage.setTargetParticipantId(participantId);
        socketMessage.setAction("PARTICIPANT_LEFT"); // Frontend nghe action này để xóa lưới Video

        // Broadcast vào phòng
        messagingTemplate.convertAndSend("/topic/meeting/" + meetingCode, socketMessage);
    }

    public String checkValidMeetingCode(String meetingCode) {
        Meeting meeting = this.meetingRepository.findByMeetingCode(meetingCode).orElseThrow(
                () -> new BusinessException(ErrorCode.ROOM_NOT_FOUND)
        );

        if (meeting.getEndTime().isBefore(LocalDateTime.now())
                || meeting.getStatus() != MeetingStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND);
        }
        return meetingCode;
    }
}