package com.meet.BE.domain.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleMeetingRequest {
    @NotBlank(message = "Title is required")
    private String title;           // Tiêu đề

    private Boolean isScheduled;    // Trạng thái đã lên lịch (giữ nguyên từ code cũ của bạn)

    @NotNull(message = "Meeting date is required")
    @FutureOrPresent(message = "Meeting date must be today or in the future")
    private LocalDate meetingDate;  // Ngày tháng năm (Ví dụ: 2026-04-17)

    @NotNull(message = "Meeting time is required")
    private LocalTime meetingTime;  // Giờ phút (Ví dụ: 14:30:00)

    @NotBlank(message = "Time zone is required")
    private String timeZone;

    private String description;     // Mô tả cuộc họp

    private List<@Email(message = "Invalid email format") String> emailList; // Danh sách email người tham dự
}
