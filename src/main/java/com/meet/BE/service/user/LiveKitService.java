package com.meet.BE.service.user;

import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.enums.Role;
import com.meet.BE.domain.enums.TrackType;
import com.meet.BE.domain.request.CreateInstantMeetingRequest;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import io.livekit.server.*;
import livekit.LivekitModels.ParticipantInfo;
import livekit.LivekitModels.TrackInfo;
import livekit.LivekitModels.TrackSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
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
                "").execute();
    }

    public void deleteRoom(String roomName) {
        try {
            roomServiceClient.deleteRoom(roomName).execute();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ROOM_DELETE_FAILED);
        }
    }

    public void createInstantRoom(CreateInstantMeetingRequest request) throws IOException {
        String meetingCode = UUID.randomUUID().toString().substring(0, 10);

        this.createLiveKitRoom(
                meetingCode,
                request.getEmptyTimeout(),
                request.getMaxParticipants());
    }

    private String buildUserToken(MeetingParticipant participant, String meetingCode) {
        AccessToken token = new AccessToken(apiKey, apiSecret);
        User user = participant.getUser();
        Role role = participant.getRole() != null ? participant.getRole() : Role.PARTICIPANT;

        token.setName(user.getFullName());
        token.setIdentity(user.getId().toString());
        token.addGrants(new VideoGrant[] {
                new RoomJoin(true),
                new RoomName(meetingCode),
                new CanPublish(true),
                new CanSubscribe(true),
                new CanUpdateOwnMetadata(true),
        });
        try {
            LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("participantId", participant.getParticipantId());
            metadata.put("role", role.name());
            metadata.put("type", "USER");
            metadata.put("meetingCode", meetingCode);
            metadata.put("avatar", user.getAvatar() == null ? "" : user.getAvatar());

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
        token.setIdentity(guest.getGuestId());
        token.addGrants(new VideoGrant[] {
                new RoomJoin(true),
                new RoomName(meetingCode),
                new CanPublish(true),
                new CanSubscribe(true),
                new CanUpdateOwnMetadata(true),
        });
        try {
            Map<String, Object> metadata = Map.of(
                    "participantId", guest.getParticipantId(),
                    "guestId", guest.getGuestId(),
                    "role", role.name(),
                    "type", "GUEST",
                    "meetingCode", meetingCode);
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

    public void removeParticipant(String roomName, String identity) {
        try {
            roomServiceClient.removeParticipant(roomName, identity).execute();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARTICIPANT_KICKED_FAILED);
        }
    }

    public void updateRoomMetadata(String roomName, String metadataJson) {
        try {
            roomServiceClient.updateRoomMetadata(roomName, metadataJson).execute();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SCREEN_SHARE_OPERATION_FAILED);
        }
    }

    public void updateParticipantMetadata(String roomName, String identity, String metadataJson) {
        try {
            roomServiceClient.updateParticipant(roomName, identity, metadataJson, null).execute();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SCREEN_SHARE_OPERATION_FAILED);
        }
    }

    public String getParticipantCurrentMetadata(String roomName, String identity) {
        try {
            ParticipantInfo info = roomServiceClient.getParticipant(roomName, identity).execute().body();
            return info != null ? info.getMetadata() : "{}";
        } catch (Exception e) {
            return "{}";
        }
    }

    public void muteScreenShareTrack(String roomName, String identity) {
        try {
            ParticipantInfo info = roomServiceClient.getParticipant(roomName, identity).execute().body();
            if (info == null) {
                throw new BusinessException(ErrorCode.USER_NOT_IN_THIS_ROOM);
            }
            String trackSid = info.getTracksList().stream()
                    .filter(track -> track.getSource() == TrackSource.SCREEN_SHARE && !track.getMuted())
                    .map(TrackInfo::getSid)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.SCREEN_SHARE_NOT_FOUND));

            roomServiceClient.mutePublishedTrack(roomName, identity, trackSid, true).execute();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SCREEN_SHARE_OPERATION_FAILED);
        }
    }

    public void muteParticipantTrack(String roomName, String identity, TrackType trackType) {
        try {
            ParticipantInfo info = roomServiceClient.getParticipant(roomName, identity).execute().body();
            if (info == null) {
                throw new BusinessException(ErrorCode.USER_NOT_IN_THIS_ROOM);
            }
            TrackSource targetSource = trackType == TrackType.AUDIO ? TrackSource.MICROPHONE : TrackSource.CAMERA;
            String trackSid = info.getTracksList().stream()
                    .filter(track -> track.getSource() == targetSource && !track.getMuted())
                    .map(TrackInfo::getSid)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_ACTIVE));

            roomServiceClient.mutePublishedTrack(roomName, identity, trackSid, true).execute();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.MUTE_OPERATION_FAILED);
        }
    }

    public void forceMuteAllNonHosts(String roomName) {
        try {
            List<ParticipantInfo> participants = roomServiceClient.listParticipants(roomName).execute().body();
            if (participants == null) return;
            for (ParticipantInfo participant : participants) {
                String metadata = participant.getMetadata();
                if (metadata == null || metadata.isBlank()) continue;
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> meta = (Map<String, Object>) objectMapper.readValue(metadata, Map.class);
                    if (Role.HOST.name().equals(meta.get("role"))) continue;
                } catch (Exception e) {
                    continue;
                }
                for (TrackInfo track : participant.getTracksList()) {
                    if (track.getSource() == TrackSource.MICROPHONE && !track.getMuted()) {
                        try {
                            roomServiceClient.mutePublishedTrack(roomName, participant.getIdentity(), track.getSid(), true).execute();
                        } catch (Exception e) {
                            log.warn("Failed to mute audio track for {} in room {}", participant.getIdentity(), roomName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to force mute non-host participants in room: {}", roomName);
        }
    }

    public List<String> getScreenSharingIdentities(String roomName) {
        try {
            List<ParticipantInfo> participants = roomServiceClient.listParticipants(roomName).execute().body();
            if (participants == null) return List.of();
            return participants.stream()
                    .filter(p -> p.getTracksList().stream()
                            .anyMatch(t -> t.getSource() == TrackSource.SCREEN_SHARE && !t.getMuted()))
                    .map(ParticipantInfo::getIdentity)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to list screen sharing participants for room: {}", roomName);
            return List.of();
        }
    }
}
