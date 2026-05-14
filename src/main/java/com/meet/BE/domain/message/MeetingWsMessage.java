package com.meet.BE.domain.message;

import lombok.Data;

@Data
public class MeetingWsMessage {
    private String meetingCode;
    private Long targetParticipantId; // ID của người đang xin vào (hoặc được duyệt/từ chối)
    private String targetName;        // Tên để hiển thị cho Host (VD: "Nguyễn Văn A đang xin vào")
    private String action;            // JOIN_REQUEST, ADMITTED, REJECTED
    private Boolean isBan;            // Đánh dấu xem có phải Ban (Hard Kick) không
}