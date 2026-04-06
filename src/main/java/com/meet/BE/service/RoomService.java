package com.meet.BE.service;

import com.meet.BE.domain.entity.Room;
import com.meet.BE.domain.request.CreateRoomRequest;
import com.meet.BE.domain.request.JoinRoomRequest;
import com.meet.BE.domain.response.RoomConnectionResponse;
import com.meet.BE.domain.status.RoomStatus;
import com.meet.BE.repository.MeetingRepository;
import com.meet.BE.utils.GenerateRoomCode;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import io.livekit.server.*;
import livekit.LivekitModels;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.Response;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RoomService {

    @Value("${livekit.api-key}")
    private String apiKey;

    @Value("${livekit.api-secret}")
    private String apiSecret;

    @Value("${livekit.host}")
    private String livekitHost;

    @Value("${livekit.ws-url}")
    private String livekitWsUrl;

    private final MeetingRepository meetingRepository;

    private RoomServiceClient roomServiceClient() {
        return RoomServiceClient.createClient(livekitHost, apiKey, apiSecret);
    }

    private String createParticipantToken(String roomCode, String participantName, boolean isHost) {
        String identity = participantName + "-" + UUID.randomUUID().toString().substring(0, 8);

        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setName(participantName);
        token.setIdentity(identity);

        // token hết hạn sau 2 giờ
        token.setTtl(TimeUnit.HOURS.toMillis(2));

        // 1. Cấp các quyền cơ bản mà ai vào phòng Meet cũng cần có
        token.addGrants(
                new RoomJoin(true),
                new RoomName(roomCode),
                new CanPublish(true),     // Cho phép bật cam/mic
                new CanSubscribe(true),   // Cho phép xem cam/mic của người khác
                new CanPublishData(true)  // Cho phép chat hoặc gửi tín hiệu (giơ tay...)
        );

        // 2. Nếu là người tạo phòng, cấp thêm quyền Quản trị (RoomAdmin)
        if (isHost) {
            token.addGrants(new RoomAdmin(true));
        }

        return token.toJwt();
    }

    public RoomConnectionResponse createRoomAndToken(CreateRoomRequest request) {
        // Fix host name, then update use security
        try {
            String roomCode = GenerateRoomCode.generateCode();
            Room newRoom = new Room();
            newRoom.setRoomCode(roomCode);
            newRoom.setDepartureTimeout(300);
            newRoom.setMetadata(null);
            newRoom.setEmptyTimeout(900);
            newRoom.setStatus(RoomStatus.active);
            newRoom.setMaxParticipants(2);

            Response<LivekitModels.Room> roomByLivekit = roomServiceClient()
                    .createRoom(
                            roomCode,
                            newRoom.getEmptyTimeout(),
                            newRoom.getMaxParticipants(),
                            null,
                            newRoom.getMetadata(),
                            null,
                            null,
                            null,
                            newRoom.getDepartureTimeout()

                    ).execute();

            if (!roomByLivekit.isSuccessful()) {
                String errorBody = roomByLivekit.errorBody() != null ? roomByLivekit.errorBody().string() : "unknown error";
                System.err.println(errorBody);
                throw new BusinessException(ErrorCode.ROOM_CANNOT_BE_CREATED);
            }

            String token = createParticipantToken(roomCode, request.getParticipantName(), true);
            this.meetingRepository.save(newRoom);

            return new RoomConnectionResponse(
                    roomCode,
                    request.getParticipantName(),
                    token,
                    livekitWsUrl
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public RoomConnectionResponse joinRoom(JoinRoomRequest request) {
        Room existingRoom = (Room) meetingRepository.findByRoomCode(request.getRoomCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        String token = createParticipantToken(request.getRoomCode(), request.getParticipantName(), false);
        return new RoomConnectionResponse(
                request.getRoomCode(),
                request.getParticipantName(),
                token,
                livekitWsUrl
        );
    }


}