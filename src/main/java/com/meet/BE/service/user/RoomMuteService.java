package com.meet.BE.service.user;

import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomMuteService {

    private final LiveKitService liveKitService;
    private final NotificationService notificationService;
    private final ParticipantEntityService participantEntityService;
    private final ParticipantPermissionService permissionService;
    private final RoomMetadataService roomMetadataService;

    public void updateMuteSetting(String meetingCode, Long hostParticipantId, boolean allowParticipantUnmute) {
        MeetingParticipant host = participantEntityService.getParticipantInMeeting(hostParticipantId, meetingCode);
        permissionService.assertHostPermission(host);

        boolean allowParticipantShareScreen;
        try {
            Map<String, Object> roomMetadata = roomMetadataService.updateRoomMetadata(
                    meetingCode,
                    metadata -> metadata.put("allowParticipantUnmute", allowParticipantUnmute));
            allowParticipantShareScreen = Boolean.TRUE.equals(roomMetadata.getOrDefault("allowParticipantShareScreen", true));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.MUTE_OPERATION_FAILED);
        }

        notificationService.sendRoomSettingsChanged(meetingCode, allowParticipantShareScreen, allowParticipantUnmute);

        if (!allowParticipantUnmute) {
            liveKitService.forceMuteAllNonHosts(meetingCode);
        }
    }
}
