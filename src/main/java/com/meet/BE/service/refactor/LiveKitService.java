package com.meet.BE.service.refactor;

import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.enums.Role;
import com.meet.BE.domain.request.CreateInstantMeetingRequest;
import io.livekit.server.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LiveKitService {

    @Value("${livekit.api-key}")
    private String apiKey;

    @Value("${livekit.api-secret}")
    private String apiSecret;

    private final RoomServiceClient roomServiceClient;
    private final ObjectMapper objectMapper;

    private void createLiveKitRoom(String meetingCode, Integer emptyTimeout, Integer maxParticipants)
            throws IOException {
        roomServiceClient.createRoom(
                meetingCode,
                emptyTimeout,
                maxParticipants,
                "",
                ""
        ).execute();
    }

    public void createInstantRoom(CreateInstantMeetingRequest request) throws IOException {
        String meetingCode = UUID.randomUUID().toString().substring(0, 10);

        this.createLiveKitRoom(
                meetingCode,
                request.getEmptyTimeout(),
                request.getMaxParticipants()
        );
    }




    private String buildUserToken(MeetingParticipant participant, String meetingCode) {
        AccessToken token = new AccessToken(apiKey, apiSecret);
        User user = participant.getUser();
        Role role = participant.getRole() != null ? participant.getRole() : Role.PARTICIPANT;

        token.setName(user.getFullName());
        token.setIdentity("user-" + user.getId().toString());
        token.addGrants(new VideoGrant[]{
                new RoomJoin(true),
                new RoomName(meetingCode),
                new CanPublish(role == Role.HOST || role == Role.PARTICIPANT),
                new CanSubscribe(true),
                new CanUpdateOwnMetadata(true),
        });
        try {
            Map<String, Object> metadata = Map.of(
                    "participantId", participant.getParticipantId(),
                    "role", role.name(),
                    "type", "USER",
                    "meetingCode", meetingCode
            );
            token.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            throw new RuntimeException("Cannot build metadata", e);
        }
        return token.toJwt();
    }

    private String buildGuestToken(MeetingParticipant guest, String meetingCode) {
        AccessToken token = new AccessToken(apiKey, apiSecret);
        Role role = Role.PARTICIPANT;

        token.setName(guest.getGuestName());
        token.setIdentity("guest-" + guest.getGuestId());
        token.addGrants(new VideoGrant[]{
                new RoomJoin(true),
                new RoomName(meetingCode),
                new CanPublish(true),
                new CanSubscribe(true),
                new CanUpdateOwnMetadata(true),
        });
        try {
            Map<String, Object> metadata = Map.of(
                    "participantId", guest.getGuestId(),
                    "role", role.name(),
                    "type", "GUEST",
                    "meetingCode", meetingCode
            );
            token.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            throw new RuntimeException("Cannot build metadata", e);
        }
        return token.toJwt();
    }



    public String buildLiveKitToken(MeetingParticipant participant, String meetingCode) {
        if (participant.getUser() != null) {
            return buildUserToken(participant, meetingCode);
        }
        return buildGuestToken(participant, meetingCode);
    }
}