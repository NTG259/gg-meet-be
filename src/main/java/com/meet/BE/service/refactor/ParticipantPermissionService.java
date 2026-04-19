package com.meet.BE.service.refactor;


import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.domain.enums.ParticipantStatus;
import com.meet.BE.domain.enums.Role;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParticipantPermissionService {
    public void assertHostPermission(MeetingParticipant actor) {
        if (actor.getRole() != Role.HOST) {
            throw new BusinessException(ErrorCode.NOT_PERMIT);
        }

        if (actor.getParticipantStatus() != ParticipantStatus.ACCEPT) {
            throw new BusinessException(ErrorCode.NOT_PERMIT);
        }
    }
}
