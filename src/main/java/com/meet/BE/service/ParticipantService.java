package com.meet.BE.service;

import com.meet.BE.domain.response.ParticipantsResponseDTO;
import com.meet.BE.repository.MeetingParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParticipantService {
    private final MeetingParticipantRepository meetingParticipantRepository;
//    public List<ParticipantsResponseDTO> getAllParticipants(String meetingCode) {
//        List<ParticipantsResponseDTO> responseDTOS = new ArrayList<>();
//
//
//    }
}
