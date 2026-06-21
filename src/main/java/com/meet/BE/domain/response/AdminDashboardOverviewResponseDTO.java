package com.meet.BE.domain.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AdminDashboardOverviewResponseDTO {
    private MetricDTO totalUsers;
    private MetricDTO activeMeetings;
    private MetricDTO totalMeetingsToday;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MetricDTO {
        private Long value;
        private BigDecimal changePercent;
        private String change;
        private String trend;
    }
}
