package com.meet.BE.controller.rest;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.request.GuestJoinRequest;
import com.meet.BE.domain.request.ScheduleMeetingRequest;
import com.meet.BE.domain.response.MeetingResponseDTO;
import com.meet.BE.service.ScheduleMeetingService;
import com.meet.BE.service.refactor.MeetingEntityService;
import com.meet.BE.service.refactor.UserService;
import com.meet.BE.service.refactor.MeetingOrchestratorService;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import com.meet.BE.utils.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {
    private final ScheduleMeetingService scheduleMeetingService;
    private final MeetingEntityService meetingEntityService;
    private final MeetingOrchestratorService meetingOrchestratorService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<MeetingResponseDTO>> createMeeting(
            @RequestParam String title
    ) throws Exception {
        String currentUserEmail = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        User currentUser = this.userService.handleFindUserByEmail(currentUserEmail);
        MeetingResponseDTO response = meetingOrchestratorService.createInstantMeeting(currentUser, title);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(response, "Tạo phòng thành công", null, HttpStatus.CREATED.value())
        );
    }

    @PostMapping("/{meetingCode}/join")
    public ResponseEntity<ApiResponse<MeetingResponseDTO>> joinMeeting(
            @PathVariable("meetingCode") String meetingCode,
            @RequestBody(required = false) GuestJoinRequest guestRequest
    ) {
        // Lấy email user nếu đã đăng nhập, nếu không có thì trả về null
        String currentUserEmail = SecurityUtils.getCurrentUserLogin().orElse(null);

//        // Chuyển toàn bộ logic xử lý (User vs Guest) vào Service
//        MeetingResponseDTO response = instantMeetingService.processJoinMeeting(currentUserEmail, meetingCode, guestRequest);

        MeetingResponseDTO response = null;

        if (!currentUserEmail.equals("anonymousUser")) {
            User currentUser = this.userService.handleFindUserByEmail(currentUserEmail);
            response = this.meetingOrchestratorService.joinRoomByUser(
                    currentUser,
                    meetingCode
            );
        } else {
            response = this.meetingOrchestratorService.joinRoomByGuest(
                    guestRequest,
                    meetingCode
            );
        }
        return ResponseEntity.ok(
                new ApiResponse<>(response, "Xử lý tham gia phòng thành công", null, HttpStatus.OK.value())
        );
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> endMeeting(
            @RequestParam(value = "meetingCode") String meetingCode
    ) throws Exception {
        String currentUserEmail = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        User currentUser = this.userService.handleFindUserByEmail(currentUserEmail);
        meetingOrchestratorService.endMeetingForEverybody(currentUser, meetingCode);
        return ResponseEntity.ok(
                new ApiResponse<>(null, "Kết thúc phòng thành công", null, HttpStatus.OK.value())
        );
    }

    @DeleteMapping("/leave")
    public ResponseEntity<ApiResponse<Void>> leaveMeeting(
            @RequestParam(value = "participantId") Long participantId,
            @RequestParam(value = "meetingCode") String meetingCode
    ) throws Exception {
        // Gọi hàm leave vừa tạo
        meetingOrchestratorService.leaveMeeting(participantId, meetingCode);
        return ResponseEntity.ok(
                new ApiResponse<>(null, "Rời phòng thành công", null, HttpStatus.OK.value())
        );
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> checkValidMeetingCode(
            @RequestParam String meetingCode) {
        String verifiedMeetingCode = meetingEntityService.validateMeetingBeforeJoin(meetingCode).getMeetingCode();
        return ResponseEntity.ok().body(
                new ApiResponse<>(
                        verifiedMeetingCode,
                        "Meeting is active",
                        null,
                        HttpStatus.OK.value()
                )
        );
    }

    @PostMapping("/schedule")
    public ResponseEntity<ApiResponse<MeetingResponseDTO>> scheduleMeeting(
            @Valid @RequestBody ScheduleMeetingRequest request) throws Exception {
        String currentUserEmail = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        User currentUser = this.userService.handleFindUserByEmail(currentUserEmail);
        MeetingResponseDTO response = meetingOrchestratorService.createScheduleMeeting(currentUser,request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(
                        response,
                        "Tạo lịch chờ thành công",
                        null,
                        HttpStatus.CREATED.value()
                )
        );
    }

}