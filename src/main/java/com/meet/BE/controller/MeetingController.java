package com.meet.BE.controller;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.request.CancelJoinRequest;
import com.meet.BE.domain.request.GuestJoinRequest;
import com.meet.BE.domain.request.ScheduleMeetingRequest;
import com.meet.BE.domain.request.UpdateMeetingSettingsRequest;
import com.meet.BE.domain.response.MeetingResponseDTO;
import com.meet.BE.domain.response.PagedResponse;
import com.meet.BE.domain.response.UpcomingMeetingItemResponse;
import com.meet.BE.domain.response.WaitingRoomParticipantResponseDTO;
import com.meet.BE.service.user.MeetingEntityService;
import com.meet.BE.service.user.MeetingOrchestratorService;
import com.meet.BE.service.user.RoomMuteService;
import com.meet.BE.service.user.ScreenShareService;
import com.meet.BE.service.user.SecurityService;
import com.meet.BE.service.user.UserService;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import com.meet.BE.utils.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
        private final ScreenShareService screenShareService;
        private final RoomMuteService roomMuteService;

        @PostMapping
        public ResponseEntity<ApiResponse<MeetingResponseDTO>> createMeeting(
                        @RequestParam("title") String title) throws Exception {
                String currentUserEmail = SecurityUtils.getCurrentUserLogin()
                                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

                User currentUser = this.userService.handleFindUserByEmail(currentUserEmail);
                MeetingResponseDTO response = meetingOrchestratorService.createInstantMeeting(currentUser, title);

                return ResponseEntity.status(HttpStatus.CREATED).body(
                                new ApiResponse<>(response, "Meeting created successfully", null,
                                                HttpStatus.CREATED.value()));
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
                        @RequestParam(name = "page", defaultValue = "0") int page,
                        @RequestParam(name = "size", defaultValue = "5") int size) {
                PagedResponse<UpcomingMeetingItemResponse> response = this.meetingEntityService
                                .getUpcomingMeetings(page, size);

                return ResponseEntity.ok(
                                new ApiResponse<>(response, "Upcoming meetings fetched successfully", null,
                                                HttpStatus.OK.value()));
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

        @PatchMapping("/{meetingCode}/settings")
        public ResponseEntity<ApiResponse<Void>> updateMeetingSettings(
                        @PathVariable("meetingCode") String meetingCode,
                        @RequestHeader(value = "Meeting-Token", required = false) String meetingToken,
                        @RequestBody UpdateMeetingSettingsRequest request) {
                Long participantId = resolveParticipantIdFromMeetingToken(meetingToken);
                if (request.getAllowParticipantShareScreen() != null) {
                        screenShareService.updateScreenShareSetting(meetingCode, participantId,
                                        request.getAllowParticipantShareScreen());
                }
                if (request.getAllowParticipantUnmute() != null) {
                        roomMuteService.updateMuteSetting(meetingCode, participantId,
                                        request.getAllowParticipantUnmute());
                }
                return ResponseEntity.ok(
                                new ApiResponse<>(null, "Room settings updated", null, HttpStatus.OK.value()));
        }

        @PostMapping("/{meetingCode}/screen-share-requests")
        public ResponseEntity<ApiResponse<Void>> requestScreenShare(
                        @PathVariable("meetingCode") String meetingCode,
                        @RequestHeader(value = "Meeting-Token", required = false) String meetingToken) {
                Long participantId = resolveParticipantIdFromMeetingToken(meetingToken);
                screenShareService.requestScreenShare(meetingCode, participantId);
                return ResponseEntity.ok(
                                new ApiResponse<>(null, "Screen share request sent", null, HttpStatus.OK.value()));
        }

        @PostMapping("/{meetingCode}/screen-share-requests/{requesterId}/approve")
        public ResponseEntity<ApiResponse<Void>> approveScreenShare(
                        @PathVariable("meetingCode") String meetingCode,
                        @PathVariable("requesterId") Long requesterId,
                        @RequestHeader(value = "Meeting-Token", required = false) String meetingToken) {
                Long participantId = resolveParticipantIdFromMeetingToken(meetingToken);
                screenShareService.approveScreenShare(meetingCode, requesterId, participantId);
                return ResponseEntity.ok(
                                new ApiResponse<>(null, "Screen share approved", null, HttpStatus.OK.value()));
        }

        @PostMapping("/{meetingCode}/screen-share-requests/{requesterId}/reject")
        public ResponseEntity<ApiResponse<Void>> rejectScreenShare(
                        @PathVariable("meetingCode") String meetingCode,
                        @PathVariable("requesterId") Long requesterId,
                        @RequestHeader(value = "Meeting-Token", required = false) String meetingToken) {
                Long participantId = resolveParticipantIdFromMeetingToken(meetingToken);
                screenShareService.rejectScreenShare(meetingCode, requesterId, participantId);
                return ResponseEntity.ok(
                                new ApiResponse<>(null, "Screen share rejected", null, HttpStatus.OK.value()));
        }

        @PostMapping("/{meetingCode}/screen-share/{targetId}/stop")
        public ResponseEntity<ApiResponse<Void>> forceStopScreenShare(
                        @PathVariable("meetingCode") String meetingCode,
                        @PathVariable("targetId") Long targetId,
                        @RequestHeader(value = "Meeting-Token", required = false) String meetingToken) {
                Long participantId = resolveParticipantIdFromMeetingToken(meetingToken);
                screenShareService.forceStopScreenShare(meetingCode, targetId, participantId);
                return ResponseEntity.ok(
                                new ApiResponse<>(null, "Screen share stopped", null, HttpStatus.OK.value()));
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
