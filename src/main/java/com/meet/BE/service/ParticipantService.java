package com.meet.BE.service;

import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.enums.ParticipantStatus;
import com.meet.BE.domain.enums.Role;
import com.meet.BE.domain.message.MeetingWsMessage;
import com.meet.BE.domain.request.GuestJoinRequest;
import com.meet.BE.repository.MeetingParticipantRepository;
import com.meet.BE.repository.MeetingRepository;
import com.meet.BE.repository.UserRepository;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipantService {
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final UserRepository userRepository;
    private final MeetingRepository meetingRepository;

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Luồng join phòng:
     * - User đăng nhập: xác định bằng email từ token hệ thống.
     * - Guest: bắt buộc gửi guestId + guestName.
     * - Host: auto ACCEPT.
     * - Người thường: vào WAITING và bắn WS cho host duyệt.
     */
    @Transactional
    public MeetingParticipant handleParticipantJoin(String email, String meetingCode, GuestJoinRequest guestRequest) {
        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));

        User user = null;
        if (!email.equals("anonymousUser")) {
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        }

        // Bắt lỗi nếu không phải User cũng không truyền GuestInfo đủ
        if (user == null && (guestRequest == null
                || guestRequest.getGuestId() == null
                || guestRequest.getGuestId().isBlank()
                || guestRequest.getGuestName() == null
                || guestRequest.getGuestName().isBlank())) {
            throw new BusinessException(ErrorCode.DATA_NOT_ENOUGH);
        }

        boolean isHost = (user != null && user.getId().equals(meeting.getHost().getId()));

        // 1. Kiểm tra Participant đã tồn tại chưa
        Optional<MeetingParticipant> existingOpt = user != null
                ? meetingParticipantRepository.findByMeetingIdAndUserId(meeting.getId(), user.getId())
                : meetingParticipantRepository.findByMeetingIdAndGuestId(meeting.getId(), guestRequest.getGuestId());

        if (existingOpt.isPresent()) {
            MeetingParticipant existing = existingOpt.get();
            // Trường hợp đã từng vào trước đó:
            // - Host luôn được vào lại
            // - Người đã ACCEPT được vào lại
            // - WAITING/REJECT giữ nguyên để tránh bypass quy trình duyệt
            if (isHost || existing.getParticipantStatus() == ParticipantStatus.ACCEPT) {
                existing.setParticipantStatus(ParticipantStatus.ACCEPT);
                return meetingParticipantRepository.save(existing);
            }
            return existing; // Đang WAITING hoặc REJECTED thì giữ nguyên
        }

        // 2. Tạo mới Participant
        MeetingParticipant newParticipant = new MeetingParticipant();
        newParticipant.setMeeting(meeting);

        String displayName;
        if (user != null) {
            newParticipant.setUser(user);
            displayName = user.getFullName();
        } else {
            newParticipant.setGuestId(guestRequest.getGuestId());
            newParticipant.setGuestName(guestRequest.getGuestName());
            displayName = guestRequest.getGuestName();
        }

        // 3. Phân quyền và Trạng thái
        if (isHost) {
            newParticipant.setRole(Role.HOST);
            newParticipant.setParticipantStatus(ParticipantStatus.ACCEPT);
            return meetingParticipantRepository.save(newParticipant);
        } else {
            newParticipant.setRole(Role.PARTICIPANT);
            newParticipant.setParticipantStatus(ParticipantStatus.WAITING);
            newParticipant = meetingParticipantRepository.save(newParticipant);

            // 4. BẮN THÔNG BÁO WEBSOCKET CHO HOST (Phòng chờ)
            MeetingWsMessage socketMessage = new MeetingWsMessage();
            socketMessage.setMeetingCode(meetingCode);
            socketMessage.setTargetParticipantId(newParticipant.getParticipantId());
            socketMessage.setTargetName(displayName);
            socketMessage.setAction("JOIN_REQUEST");

            messagingTemplate.convertAndSend("/topic/meeting/" + meetingCode + "/waiting", socketMessage);

            return newParticipant;
        }
    }

    @Transactional
    // Host duyệt vào phòng: bắt buộc actor là HOST của đúng meetingCode.
    public MeetingParticipant admit(Long actorParticipantId, Long participantId, String meetingCode) {
        assertHostActionPermission(actorParticipantId, meetingCode);

        // 1. Tìm trực tiếp bằng Khóa chính (Nhanh nhất, hỗ trợ cả User lẫn Guest)
        MeetingParticipant participant = meetingParticipantRepository.findById(participantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));

        // 2. [QUAN TRỌNG] Kiểm tra chéo: Người này có thực sự đang xin vào phòng này không?
        if (!participant.getMeeting().getMeetingCode().equals(meetingCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_IN_THIS_ROOM);
        }

        // 3. Kiểm tra trạng thái
        if (participant.getParticipantStatus() != ParticipantStatus.WAITING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }

        participant.setParticipantStatus(ParticipantStatus.ACCEPT);
        return meetingParticipantRepository.save(participant);
    }

    @Transactional
    public MeetingParticipant reject(Long actorParticipantId, Long participantId, String meetingCode) {
        // Kiểm tra xem có đúng là host không ? Nếu không phải throw Exception
        assertHostActionPermission(actorParticipantId, meetingCode);

        MeetingParticipant participant = meetingParticipantRepository.findById(participantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));

        if (!participant.getMeeting().getMeetingCode().equals(meetingCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_IN_THIS_ROOM);
        }

        if (participant.getParticipantStatus() != ParticipantStatus.WAITING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }

        participant.setParticipantStatus(ParticipantStatus.REJECT);
        return meetingParticipantRepository.save(participant);
    }

    @Transactional
    public void cancel(Long targetParticipantId, String meetingCode, String targetName) {
        Optional<MeetingParticipant> participantOpt = meetingParticipantRepository.findById(targetParticipantId);
        if (participantOpt.isEmpty()) {
            return;
        }

        MeetingParticipant participant = participantOpt.get();
        if (!participant.getMeeting().getMeetingCode().equals(meetingCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_IN_THIS_ROOM);
        }

        if (participant.getParticipantStatus() != ParticipantStatus.WAITING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }

        // Lưu ý: Đoạn check này có thể bạn đang muốn check quyền của host,
        // Hãy đảm bảo logic map đúng giữa User ID và Participant ID nhé.
        if (!participant.getUser().getId().equals(targetParticipantId)) {
            throw new BusinessException(ErrorCode.NOT_PERMIT);
        }

        // 1. Xóa participant
        meetingParticipantRepository.delete(participant);

        // 2. Broadcast message sau khi xử lý database thành công
        MeetingWsMessage messageHost = new MeetingWsMessage();
        messageHost.setAction("CANCEL_SUCCESS");
        messageHost.setMeetingCode(meetingCode);
        messageHost.setTargetParticipantId(targetParticipantId);
        messageHost.setTargetName(targetName);

        Long hostParticipantId = meetingRepository.findByMeetingCode(meetingCode).orElseThrow(
                () -> new BusinessException(ErrorCode.ROOM_NOT_FOUND)
        ).getHost().getId();
        messagingTemplate.convertAndSend(
                "/topic/meeting/" + meetingCode + "/participant/" + hostParticipantId,
                messageHost
        );
        log.info("Sent cancel join to host topic: {}", hostParticipantId);
    }

    // Validate quyền thao tác của Host tại backend để tránh giả mạo payload từ FE.
    private void assertHostActionPermission(Long actorParticipantId, String meetingCode) {
        if (actorParticipantId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        MeetingParticipant actor = meetingParticipantRepository.findById(actorParticipantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (!actor.getMeeting().getMeetingCode().equals(meetingCode)) {
            throw new BusinessException(ErrorCode.NOT_PERMIT);
        }

        if (actor.getRole() != Role.HOST || actor.getParticipantStatus() != ParticipantStatus.ACCEPT) {
            throw new BusinessException(ErrorCode.NOT_PERMIT);
        }
    }
}
