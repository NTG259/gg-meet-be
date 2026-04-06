package com.meet.BE.controller;

import com.meet.BE.domain.Meeting;
import com.meet.BE.domain.dto.CreateMeetingDTO;
import com.meet.BE.domain.response.MeetingResponse;
import com.meet.BE.service.MeetingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin("*")
@RequestMapping("api/meetings")
public class MeetingController {
    @Autowired
    private MeetingService meetingService;

    @PostMapping("")
    public ResponseEntity<MeetingResponse> createMeeting(@RequestBody CreateMeetingDTO createMeetingDTO) {
        String meetingCode = this.meetingService.createMeeting(createMeetingDTO);
        String urlMeeting = "https://100.93.168.24/meet/" + meetingCode;
        MeetingResponse response = new MeetingResponse(meetingCode, urlMeeting);
        return ResponseEntity.ok().body(response);
    }


}
