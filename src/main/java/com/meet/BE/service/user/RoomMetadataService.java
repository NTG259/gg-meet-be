package com.meet.BE.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class RoomMetadataService {

    private final LiveKitService liveKitService;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, Object> roomLocks = new ConcurrentHashMap<>();

    public Map<String, Object> updateRoomMetadata(String meetingCode, Consumer<Map<String, Object>> updater)
            throws Exception {
        Object lock = roomLocks.computeIfAbsent(meetingCode, ignored -> new Object());
        synchronized (lock) {
            Map<String, Object> roomMetadata = parseOrEmptyMap(liveKitService.getRoomCurrentMetadata(meetingCode));
            updater.accept(roomMetadata);
            liveKitService.updateRoomMetadata(meetingCode, objectMapper.writeValueAsString(roomMetadata));
            return new LinkedHashMap<>(roomMetadata);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseOrEmptyMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return new LinkedHashMap<>((Map<String, Object>) objectMapper.readValue(json, Map.class));
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }
}
