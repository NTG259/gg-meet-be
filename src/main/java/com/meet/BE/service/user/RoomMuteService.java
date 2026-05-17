package com.meet.BE.service.user;

import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomMuteService {

    private final LiveKitService liveKitService;
    private final NotificationService notificationService;
    private final ParticipantEntityService participantEntityService;
    private final ParticipantPermissionService permissionService;
    private final ObjectMapper objectMapper;

    public void updateMuteSetting(String meetingCode, Long hostParticipantId, boolean allowParticipantUnmute) {
        MeetingParticipant host = participantEntityService.getParticipantInMeeting(hostParticipantId, meetingCode);
        permissionService.assertHostPermission(host);

        Map<String, Object> roomMetadata = new LinkedHashMap<>();
        roomMetadata.put("allowParticipantUnmute", allowParticipantUnmute);

        try {
            String metadataJson = objectMapper.writeValueAsString(roomMetadata);
            liveKitService.updateRoomMetadata(meetingCode, metadataJson);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.MUTE_OPERATION_FAILED);
        }

        notificationService.sendMuteSettingsChanged(meetingCode, allowParticipantUnmute);

        if (!allowParticipantUnmute) {
            liveKitService.forceMuteAllNonHosts(meetingCode);
        }
    }
}
