package com.meet.BE.service.user;

import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.domain.enums.TrackType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TargetedMuteService {

    private final LiveKitService liveKitService;
    private final ParticipantEntityService participantEntityService;
    private final ParticipantPermissionService permissionService;

    public void muteParticipant(String meetingCode, Long targetParticipantId, Long hostParticipantId, TrackType trackType) {
        MeetingParticipant host = participantEntityService.getParticipantInMeeting(hostParticipantId, meetingCode);
        permissionService.assertHostPermission(host);

        MeetingParticipant target = participantEntityService.getParticipantInMeeting(targetParticipantId, meetingCode);
        String identity = liveKitService.buildIdentity(target);

        liveKitService.muteParticipantTrack(meetingCode, identity, trackType);
    }
}
