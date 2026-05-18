package com.meet.BE.service.user;

import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScreenShareService {

    private final LiveKitService liveKitService;
    private final NotificationService notificationService;
    private final ParticipantEntityService participantEntityService;
    private final ParticipantPermissionService permissionService;
    private final ObjectMapper objectMapper;

    public void updateScreenShareSetting(String meetingCode, Long hostParticipantId, boolean allowParticipantShareScreen) {
        MeetingParticipant host = participantEntityService.getParticipantInMeeting(hostParticipantId, meetingCode);
        permissionService.assertHostPermission(host);

        Map<String, Object> roomMetadata = new LinkedHashMap<>();
        roomMetadata.put("allowParticipantShareScreen", allowParticipantShareScreen);

        try {
            String metadataJson = objectMapper.writeValueAsString(roomMetadata);
            liveKitService.updateRoomMetadata(meetingCode, metadataJson);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SCREEN_SHARE_OPERATION_FAILED);
        }

        notificationService.sendRoomSettingsChanged(meetingCode, allowParticipantShareScreen);

        if (!allowParticipantShareScreen) {
            forceStopAllScreenShares(meetingCode);
        }
    }

    public void requestScreenShare(String meetingCode, Long requesterParticipantId) {
        MeetingParticipant requester = participantEntityService.getParticipantInMeeting(requesterParticipantId, meetingCode);
        String requesterName = resolveParticipantName(requester);
        notificationService.sendScreenShareRequested(meetingCode, requesterParticipantId, requesterName);
    }

    public void approveScreenShare(String meetingCode, Long requesterId, Long hostParticipantId) {
        MeetingParticipant host = participantEntityService.getParticipantInMeeting(hostParticipantId, meetingCode);
        permissionService.assertHostPermission(host);

        MeetingParticipant requester = participantEntityService.getParticipantInMeeting(requesterId, meetingCode);
        String identity = liveKitService.buildIdentity(requester);

        updateCanShareScreenOverride(meetingCode, identity, true);

        notificationService.sendScreenShareApproved(meetingCode, requesterId);
    }

    public void rejectScreenShare(String meetingCode, Long requesterId, Long hostParticipantId) {
        MeetingParticipant host = participantEntityService.getParticipantInMeeting(hostParticipantId, meetingCode);
        permissionService.assertHostPermission(host);

        notificationService.sendScreenShareRejected(meetingCode, requesterId);
    }

    public void forceStopScreenShare(String meetingCode, Long targetParticipantId, Long hostParticipantId) {
        MeetingParticipant host = participantEntityService.getParticipantInMeeting(hostParticipantId, meetingCode);
        permissionService.assertHostPermission(host);

        MeetingParticipant target = participantEntityService.getParticipantInMeeting(targetParticipantId, meetingCode);
        String identity = liveKitService.buildIdentity(target);

        liveKitService.muteScreenShareTrack(meetingCode, identity);

        updateCanShareScreenOverride(meetingCode, identity, false);

        notificationService.sendScreenShareStopped(meetingCode, targetParticipantId);
    }

    private void forceStopAllScreenShares(String meetingCode) {
        List<String> sharingIdentities = liveKitService.getScreenSharingIdentities(meetingCode);
        for (String identity : sharingIdentities) {
            try {
                liveKitService.muteScreenShareTrack(meetingCode, identity);
                log.info("Force stopped screen share for identity: {} in room: {}", identity, meetingCode);
            } catch (BusinessException e) {
                log.warn("Failed to force stop screen share for identity: {} in room: {}", identity, meetingCode);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void updateCanShareScreenOverride(String meetingCode, String identity, boolean canShare) {
        try {
            String currentMetadata = liveKitService.getParticipantCurrentMetadata(meetingCode, identity);
            Map<String, Object> metadata;
            if (currentMetadata == null || currentMetadata.isBlank()) {
                metadata = new LinkedHashMap<>();
            } else {
                metadata = (Map<String, Object>) objectMapper.readValue(currentMetadata, Map.class);
            }
            metadata.put("canShareScreenOverride", canShare);
            String newMetadata = objectMapper.writeValueAsString(metadata);
            liveKitService.updateParticipantMetadata(meetingCode, identity, newMetadata);
        } catch (Exception e) {
            log.warn("Failed to update canShareScreenOverride to {} for identity: {} in room: {}",
                    canShare, identity, meetingCode);
        }
    }

    private String resolveParticipantName(MeetingParticipant participant) {
        if (participant.getGuestName() != null && !participant.getGuestName().isBlank()) {
            return participant.getGuestName();
        }
        if (participant.getUser() != null) {
            return participant.getUser().getFullName();
        }
        return participant.getEmail();
    }
}
