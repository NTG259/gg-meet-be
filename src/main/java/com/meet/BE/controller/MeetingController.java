package com.meet.BE.controller;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.request.CancelJoinRequest;
import com.meet.BE.domain.request.GuestJoinRequest;
import com.meet.BE.domain.request.ScheduleMeetingRequest;
import com.meet.BE.domain.response.MeetingResponseDTO;
import com.meet.BE.domain.response.PagedResponse;
import com.meet.BE.domain.response.UpcomingMeetingItemResponse;
import com.meet.BE.domain.response.WaitingRoomParticipantResponseDTO;
import com.meet.BE.service.refactor.MeetingEntityService;
import com.meet.BE.service.refactor.MeetingOrchestratorService;
import com.meet.BE.service.refactor.SecurityService;
import com.meet.BE.service.refactor.UserService;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import com.meet.BE.utils.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Slf4j
public class MeetingController {
    private final MeetingEntityService meetingEntityService;
    private final MeetingOrchestratorService meetingOrchestratorService;
    private final SecurityService securityService;
    private final UserService userService;
    private final StringRedisTemplate redisTemplate;

    public void test() {
        redisTemplate.opsForValue().set("test", "hello");
        String value = redisTemplate.opsForValue().get("test");
        System.out.println(value);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MeetingResponseDTO>> createMeeting(
            @RequestParam("title") String title) throws Exception {
        String currentUserEmail = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        User currentUser = this.userService.handleFindUserByEmail(currentUserEmail);
        MeetingResponseDTO response = meetingOrchestratorService.createInstantMeeting(currentUser, title);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(response, "Meeting created successfully", null, HttpStatus.CREATED.value()));
    }

    @PostMapping("/{meetingCode}/join")
    public ResponseEntity<ApiResponse<MeetingResponseDTO>> joinMeeting(
            @PathVariable("meetingCode") String meetingCode,
            @RequestBody(required = false) GuestJoinRequest guestRequest) {
        String currentUserEmail = SecurityUtils.getCurrentUserLogin().orElse("anonymousUser");
        log.info(
                "Join request received: meetingCode={}, principal={}, guestId={}, guestName={}",
                meetingCode,
                currentUserEmail,
                guestRequest != null ? guestRequest.getGuestId() : null,
                guestRequest != null ? guestRequest.getGuestName() : null);

        MeetingResponseDTO response;

        if (!"anonymousUser".equals(currentUserEmail)) {
            log.info("Join request routed as authenticated user: meetingCode={}, email={}", meetingCode,
                    currentUserEmail);
            User currentUser = this.userService.handleFindUserByEmail(currentUserEmail);
            response = this.meetingOrchestratorService.joinRoomByUser(
                    currentUser,
                    meetingCode);
        } else {
            log.info("Join request routed as guest: meetingCode={}", meetingCode);
            response = this.meetingOrchestratorService.joinRoomByGuest(
                    guestRequest,
                    meetingCode);
        }
        return ResponseEntity.ok(
                new ApiResponse<>(response, "Join request processed successfully", null,
                        HttpStatus.OK.value()));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<PagedResponse<UpcomingMeetingItemResponse>>> getUpcomingMeetings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<UpcomingMeetingItemResponse> response = this.meetingEntityService.getUpcomingMeetings(page, size);

        return ResponseEntity.ok(
                new ApiResponse<>(response, "Upcoming meetings fetched successfully", null, HttpStatus.OK.value()));
    }

    @GetMapping("/{meetingCode}/waiting-room")
    public ResponseEntity<ApiResponse<List<WaitingRoomParticipantResponseDTO>>> getWaitingRoomRequests(
            @PathVariable("meetingCode") String meetingCode,
            @RequestHeader(value = "Meeting-Token", required = false) String meetingToken) {
        Long participantId = resolveParticipantIdFromMeetingToken(meetingToken);
        List<WaitingRoomParticipantResponseDTO> response = this.meetingOrchestratorService
                .getWaitingParticipants(meetingCode, participantId);

        return ResponseEntity.ok(
                new ApiResponse<>(response, "Waiting-room requests synced", null,
                        HttpStatus.OK.value()));
    }

    @GetMapping("/{meetingCode}/join-status")
    public ResponseEntity<ApiResponse<MeetingResponseDTO>> getJoinRequestStatus(
            @PathVariable("meetingCode") String meetingCode,
            @RequestHeader(value = "Meeting-Token", required = false) String meetingToken) {
        Long participantId = resolveParticipantIdFromMeetingToken(meetingToken);
        MeetingResponseDTO response = this.meetingOrchestratorService.getJoinRequestStatus(meetingCode,
                participantId);

        return ResponseEntity.ok(
                new ApiResponse<>(response, "Join request status synced", null, HttpStatus.OK.value()));
    }

    @PostMapping("/{meetingCode}/cancel-join")
    public ResponseEntity<ApiResponse<Void>> cancelJoin(
            @PathVariable("meetingCode") String meetingCode,
            @RequestBody(required = false) CancelJoinRequest request) {
        String meetingToken = request != null ? request.getMeetingToken() : null;
        Long participantId = resolveParticipantIdFromMeetingToken(meetingToken);
        this.meetingOrchestratorService.cancelJoin(participantId, meetingCode);

        return ResponseEntity.ok(
                new ApiResponse<>(null, "Join request cancelled", null, HttpStatus.OK.value()));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> endMeeting(
            @RequestParam(value = "meetingCode") String meetingCode) throws Exception {
        String currentUserEmail = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        User currentUser = this.userService.handleFindUserByEmail(currentUserEmail);
        meetingOrchestratorService.endMeetingForEverybody(currentUser, meetingCode);
        return ResponseEntity.ok(
                new ApiResponse<>(null, "End request success", null, HttpStatus.OK.value()));
    }

    @DeleteMapping("/leave")
    public ResponseEntity<ApiResponse<Void>> leaveMeeting(
            @RequestParam(value = "participantId") Long participantId,
            @RequestParam(value = "meetingCode") String meetingCode) throws Exception {
        meetingOrchestratorService.leaveMeeting(participantId, meetingCode);
        return ResponseEntity.ok(
                new ApiResponse<>(null, "Leave request success", null, HttpStatus.OK.value()));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> checkValidMeetingCode(
            @RequestParam("meetingCode") String meetingCode) {
        String verifiedMeetingCode = meetingEntityService.validateMeetingBeforeJoin(meetingCode)
                .getMeetingCode();
        return ResponseEntity.ok().body(
                new ApiResponse<>(
                        verifiedMeetingCode,
                        "Meeting is active",
                        null,
                        HttpStatus.OK.value()));
    }

    @PostMapping("/schedule")
    public ResponseEntity<ApiResponse<MeetingResponseDTO>> scheduleMeeting(
            @Valid @RequestBody ScheduleMeetingRequest request) throws Exception {
        String currentUserEmail = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        User currentUser = this.userService.handleFindUserByEmail(currentUserEmail);
        MeetingResponseDTO response = meetingOrchestratorService.createScheduleMeeting(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(
                        response,
                        "Schedule request success",
                        null,
                        HttpStatus.CREATED.value()));
    }

    private Long resolveParticipantIdFromMeetingToken(String meetingToken) {
        if (meetingToken == null || meetingToken.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        try {
            return this.securityService.getParticipantIdFromToken(meetingToken);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
