package com.meet.BE.controller.rest;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.request.GuestJoinRequest;
import com.meet.BE.domain.request.ScheduleMeetingRequest;
import com.meet.BE.domain.response.MeetingResponseDTO;
import com.meet.BE.service.InstantMeetingService;
import com.meet.BE.service.ScheduleMeetingService;
import com.meet.BE.service.UserService;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import com.meet.BE.utils.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {
    private final ScheduleMeetingService scheduleMeetingService;
    private final InstantMeetingService instantMeetingService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<MeetingResponseDTO>> createMeeting(
            @RequestParam String title
    ) throws Exception {
        String currentUserEmail = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        User currentUser = this.userService.handleFindUserByEmail(currentUserEmail);
        MeetingResponseDTO response = instantMeetingService.createMeeting(currentUser, title);

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

        // Chuyển toàn bộ logic xử lý (User vs Guest) vào Service
        MeetingResponseDTO response = instantMeetingService.processJoinMeeting(currentUserEmail, meetingCode, guestRequest);

        return ResponseEntity.ok(
                new ApiResponse<>(response, "Xử lý tham gia phòng thành công", null, HttpStatus.OK.value())
        );
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> endMeeting(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(value = "meetingCode") String meetingCode
    ) throws Exception {
        instantMeetingService.endMeeting(currentUser, meetingCode);
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
        instantMeetingService.leaveMeeting(participantId, meetingCode);
        return ResponseEntity.ok(
                new ApiResponse<>(null, "Rời phòng thành công", null, HttpStatus.OK.value())
        );
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> checkValidMeetingCode(
            @RequestParam String meetingCode) {
        String verifiedMeetingCode = instantMeetingService.checkValidMeetingCode(meetingCode);
        return ResponseEntity.ok().body(
                new ApiResponse<>(
                        verifiedMeetingCode,
                        "Meeting code valid",
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
        MeetingResponseDTO response = scheduleMeetingService.createSchedule(currentUser, request);
        return ResponseEntity.ok().body(
                new ApiResponse<>(
                        response,
                        "Tạo lịch chờ thành công",
                        null,
                        HttpStatus.CREATED.value()
                )
        );
    }

}