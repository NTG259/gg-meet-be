package com.meet.BE.service.refactor;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final StringRedisTemplate redisTemplate;
    private static final String KEY = "meeting_schedule";

    public void pushToSchedule(Long meetingId, long startTimeEpoch) {
        redisTemplate.opsForZSet().add(
                KEY,
                meetingId.toString(),
                startTimeEpoch
        );
    }

    public Set<String> getReadyMeetings(long currentTimeEpoch) {
        return redisTemplate.opsForZSet()
                .rangeByScore(KEY, 0, currentTimeEpoch);
    }

    public void removeFromSchedule(String meetingId) {
        redisTemplate.opsForZSet()
                .remove(KEY, meetingId);
    }

    public boolean tryLock(String meetingId, long expireSeconds) {
        Boolean lock = redisTemplate.opsForValue()
                .setIfAbsent(
                        "lock:meeting:" + meetingId,
                        "1",
                        Duration.ofSeconds(expireSeconds)
                );

        return Boolean.TRUE.equals(lock);
    }
}
