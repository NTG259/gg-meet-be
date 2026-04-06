package com.meet.BE.service;


import com.meet.BE.domain.Meeting;
import com.meet.BE.domain.dto.CreateMeetingDTO;
import com.meet.BE.repository.MeetingRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MeetingService {
    @Autowired
    private MeetingRepository meetingRepository;

    @Transactional
    public String createMeeting(CreateMeetingDTO createMeetingDTO) {
        Meeting meeting = Meeting.builder()
                .id(UUID.randomUUID())
                .isScheduled(false)
                .hostId(createMeetingDTO.getHostId())
                .status("waiting")
                .build();
        var rs = meetingRepository.save(meeting);
        return  rs.getMeetingCode();
    }
}
