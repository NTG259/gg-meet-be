package com.meet.BE.utils.schedule;

import com.meet.BE.service.refactor.MeetingOrchestratorService;
import com.meet.BE.service.refactor.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleJob {

    private final ScheduleService scheduleService;
    private final MeetingOrchestratorService meetingOrchestratorService;

    @Scheduled(fixedRate = 2000)
    public void run() {

        long now = Instant.now().getEpochSecond();

        Set<String> readyMeetings = scheduleService.getReadyMeetings(now);
        log.warn("READY = {}", readyMeetings);
        if (readyMeetings == null || readyMeetings.isEmpty()) return;

        for (String meetingId : readyMeetings) {

            if (!scheduleService.tryLock(meetingId, 60)) {
                log.debug("Skip meeting {} - locked by another instance", meetingId);
                continue;
            }

            try {
                Long id = Long.parseLong(meetingId);

                log.info("Activating meeting {}", meetingId);

                meetingOrchestratorService.activeMeeting(id);

                scheduleService.removeFromSchedule(meetingId);

                log.info("Activated & removed meeting {}", meetingId);

            } catch (Exception e) {
                log.error("Failed to process meeting {}", meetingId, e);
            }
        }
    }
}