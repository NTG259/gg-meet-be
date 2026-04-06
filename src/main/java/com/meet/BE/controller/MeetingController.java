package com.meet.BE.controller;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.request.CreateMeetingRequest;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.response.MeetingResponseDTO;
import com.meet.BE.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping
    public ResponseEntity<ApiResponse<MeetingResponseDTO>> createMeeting(
//            @AuthenticationPrincipal User currentUser,
            @RequestBody CreateMeetingRequest request
    ) throws Exception {
        // TODO: Xóa mock user khi tích hợp Security
        User testHost = new User();
        testHost.setId(1L);
        testHost.setFullName("Host User"); // Bổ sung tên để tạo LiveKit Token

        MeetingResponseDTO response = meetingService.createMeeting(testHost, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(
                        response,
                        "Tạo phòng thành công",
                        null,
                        HttpStatus.CREATED.value()
                )
        );
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<MeetingResponseDTO>> joinMeeting(
//            @AuthenticationPrincipal User currentUser,
            @RequestParam(value = "meetingCode") String meetingCode
    ) {
        // TODO: Xóa mock user khi tích hợp Security
        User testParticipant = new User();
        testParticipant.setId(2L); // Giả lập một user khác (ID = 2) tham gia
        testParticipant.setFullName("Participant User"); // Bổ sung tên để tạo LiveKit Token

        MeetingResponseDTO response = meetingService.joinMeeting(testParticipant, meetingCode);
        return ResponseEntity.ok(
                new ApiResponse<>(
                        response,
                        "Tham gia phòng thành công",
                        null,
                        HttpStatus.OK.value()
                )
        );
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> endMeeting(
//            @AuthenticationPrincipal User currentUser,
            @RequestParam(value = "meetingCode") String meetingCode
    ) throws Exception {
        // TODO: Xóa mock user khi tích hợp Security (Lấy user từ Token)
        User testHost = new User();
        testHost.setId(1L);
        meetingService.endMeeting(testHost, meetingCode);
        return ResponseEntity.ok(
                new ApiResponse<>(
                        null,
                        "Kết thúc phòng thành công",
                        null,
                        HttpStatus.OK.value()
                )
        );
    }
}