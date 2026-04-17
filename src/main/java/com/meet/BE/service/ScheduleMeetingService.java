package com.meet.BE.service;

import com.meet.BE.domain.entity.Meeting;
import com.meet.BE.domain.entity.MeetingParticipant;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.enums.MeetingStatus;
import com.meet.BE.domain.enums.ParticipantStatus;
import com.meet.BE.domain.enums.Role;
import com.meet.BE.domain.request.ScheduleMeetingRequest;
import com.meet.BE.domain.response.MeetingResponseDTO;
import com.meet.BE.repository.MeetingParticipantRepository;
import com.meet.BE.repository.MeetingRepository;
import com.meet.BE.utils.convert.MeetingConvert;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleMeetingService {
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingRepository meetingRepository;
    private final EmailService emailService;
    private final UserService userService;

    @Value("${app.frontend.base-url:http://100.93.168.24:3000}")
    private String frontendBaseUrl;

    @Transactional(rollbackFor = Exception.class)
    public MeetingResponseDTO createSchedule(User host, ScheduleMeetingRequest request) throws Exception {
        String meetingCode = UUID.randomUUID().toString().substring(0, 10);

        Meeting meeting = new Meeting();
        meeting.setHost(host);
        meeting.setMeetingCode(meetingCode);
        meeting.setTitle(request.getTitle());
        meeting.setDescription(request.getDescription());
        meeting.setStatus(MeetingStatus.SCHEDULED);
        meeting.setStartTime(LocalDateTime.of(request.getMeetingDate(), request.getMeetingTime()));
        meeting.setRoomSid(meetingCode);
        Meeting savedMeeting = meetingRepository.save(meeting);

        // 4. Lưu Host vào bảng MeetingParticipant
        MeetingParticipant hostParticipant = new MeetingParticipant();
        hostParticipant.setMeeting(savedMeeting);
        hostParticipant.setUser(host);
        hostParticipant.setRole(Role.HOST);
        hostParticipant.setParticipantStatus(ParticipantStatus.ACCEPT);
        hostParticipant = meetingParticipantRepository.save(hostParticipant);

        // 5. Gửi thư mời
        if (request.getEmailList() != null && !request.getEmailList().isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
            String formattedTime = LocalDateTime
                    .of(request.getMeetingDate(), request.getMeetingTime())
                    .format(formatter);
            String joinLink = frontendBaseUrl + "/meeting/" + meetingCode;

            for (String email : request.getEmailList()) {
                emailService.sendMeetingInvitation(
                        email,
                        host.getFullName(),
                        request.getTitle(),
                        formattedTime,
                        joinLink,
                        meetingCode
                );
            }
        }

        // 6. Trả về DTO
        MeetingResponseDTO response = MeetingConvert.ToResponse(savedMeeting, null);
        response.setParticipantStatus(ParticipantStatus.ACCEPT);
        response.setTitle(savedMeeting.getTitle());
        response.setStartTime(LocalDateTime.of(request.getMeetingDate(), request.getMeetingTime()));

        return response;
    }
}
