package com.meet.BE.service;

import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.domain.enums.ParticipantStatus;
import com.meet.BE.domain.enums.Role;
import com.meet.BE.domain.message.MeetingMessage;
import com.meet.BE.domain.request.CreateMeetingRequest;
import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.response.MeetingResponseDTO;
import com.meet.BE.domain.enums.MeetingStatus;
import com.meet.BE.repository.MeetingParticipantRepository;
import com.meet.BE.repository.MeetingRepository;
import com.meet.BE.utils.convert.MeetingConvert;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import io.livekit.server.RoomServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingRepository meetingRepository;
    private final RoomServiceClient roomServiceClient;

    @Value("${livekit.api-key}")
    private String apiKey;

    @Value("${livekit.api-secret}")
    private String apiSecret;

    @Transactional(rollbackFor = Exception.class)
    public MeetingResponseDTO createMeeting(User host, CreateMeetingRequest request) throws Exception {
        String meetingCode = UUID.randomUUID().toString().substring(0, 10);

        livekit.LivekitRoom.CreateRoomRequest livekitRequest = livekit.LivekitRoom.CreateRoomRequest.newBuilder()
                .setName(meetingCode)
                .setEmptyTimeout(request.getEmptyTimeout())
                .setDepartureTimeout(request.getDepartureTimeout())
                .setMaxParticipants(request.getMaxParticipants())
                .setMetadata(request.getMetadata())
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
        meeting.setTitle(request.getTitle());
        meeting.setStatus(MeetingStatus.ACTIVE);
        meeting.setCreatedAt(LocalDateTime.now());
        meeting.setRoomSid(meetingCode);
        Meeting savedMeeting = meetingRepository.save(meeting);

        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setName(host.getFullName());
        token.setIdentity(host.getId().toString());
        token.addGrants(new RoomJoin(true));
        token.addGrants(new RoomName(meetingCode));

        String livekitToken = token.toJwt();

        return MeetingConvert.ToResponse(meeting, livekitToken);
    }

    /**
     * Tham gia vào một cuộc họp đã tồn tại
     */

    @Transactional
    public MeetingResponseDTO joinMeeting(User participant, String meetingCode){
        Meeting meeting = handleFindMeeting(meetingCode);

        if (meeting.getStatus() != MeetingStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND);
        }


        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setName(participant.getFullName());
        token.setIdentity(participant.getId().toString());

        token.addGrants(new RoomJoin(true));
        token.addGrants(new RoomName(meetingCode));

        String livekitToken = token.toJwt();

        MeetingParticipant meetingParticipant = new MeetingParticipant();
        meetingParticipant.setMeeting(meeting);
        meetingParticipant.setUser(participant);
        if (meeting.getHost().getId() == participant.getId()) {
            meetingParticipant.setRole(Role.HOST);
            meetingParticipant.setParticipantStatus(ParticipantStatus.ACCEPT);
        } else {
            meetingParticipant.setRole(Role.PARTICIPANT);
            meetingParticipant.setParticipantStatus(ParticipantStatus.WAITING);
        }
        this.meetingParticipantRepository.save(meetingParticipant);
        return MeetingConvert.ToResponse(meeting, livekitToken);
    }

    @Transactional(rollbackFor = Exception.class)
    public void endMeeting(User currentUser, String meetingCode) throws Exception {
        // 1. Tìm phòng trong Database
        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        // 2. Kiểm tra xem phòng còn hoạt động không
        if (meeting.getStatus() != MeetingStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND); // Hoặc tạo thêm ErrorCode.ROOM_ALREADY_ENDED
        }

        // 3. Kiểm tra quyền: Chỉ Host mới được kết thúc phòng
        if (!meeting.getHost().getId().equals(currentUser.getId())) {
            // Cần có ErrorCode cho lỗi không đủ quyền, ở đây tạm dùng một lỗi chung hoặc bạn tự định nghĩa thêm
            throw new BusinessException(ErrorCode.NOT_PERMIT);
        }

        // 4. Xóa phòng trên LiveKit Server
        try {
            roomServiceClient.deleteRoom(meetingCode).execute();
        } catch (Exception e) {
            System.err.println("LiveKit Room delete error: " + e.getMessage());
        }

        meeting.setStatus(MeetingStatus.ENDED);
        meetingRepository.save(meeting);
    }

    public Meeting handleFindMeeting(String roomCode) {
        return this.meetingRepository.findByMeetingCode(roomCode).orElseThrow(
                () -> new BusinessException(ErrorCode.DATA_NOT_FOUND)
        );
    }

}