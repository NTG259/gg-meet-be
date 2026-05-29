package com.meet.BE.service.admin;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.enums.MeetingStatus;
import com.meet.BE.domain.response.AdminDashboardOverviewResponseDTO;
import com.meet.BE.repository.MeetingRepository;
import com.meet.BE.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100).setScale(1);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(1);

    private final UserRepository userRepository;
    private final MeetingRepository meetingRepository;

    @Transactional(readOnly = true)
    public ApiResponse<AdminDashboardOverviewResponseDTO> getOverview() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();

        LocalDate firstDayOfCurrentMonth = today.withDayOfMonth(1);
        LocalDateTime currentMonthStart = firstDayOfCurrentMonth.atStartOfDay();
        LocalDateTime nextMonthStart = firstDayOfCurrentMonth.plusMonths(1).atStartOfDay();
        LocalDateTime previousMonthStart = firstDayOfCurrentMonth.minusMonths(1).atStartOfDay();

        long totalUsers = userRepository.countByIsDeletedFalse();
        long usersCreatedThisMonth = userRepository.countByIsDeletedFalseAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                currentMonthStart,
                nextMonthStart
        );
        long usersCreatedPreviousMonth = userRepository.countByIsDeletedFalseAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                previousMonthStart,
                currentMonthStart
        );

        long activeMeetings = meetingRepository.countByStatus(MeetingStatus.ACTIVE);
        long activeMeetingsToday = meetingRepository.countByStatusAndStartTimeGreaterThanEqualAndStartTimeLessThan(
                MeetingStatus.ACTIVE,
                todayStart,
                tomorrowStart
        );
        long activeMeetingsYesterday = meetingRepository.countByStatusAndStartTimeGreaterThanEqualAndStartTimeLessThan(
                MeetingStatus.ACTIVE,
                yesterdayStart,
                todayStart
        );

        long meetingsToday = meetingRepository.countByStartTimeGreaterThanEqualAndStartTimeLessThan(
                todayStart,
                tomorrowStart
        );
        long meetingsYesterday = meetingRepository.countByStartTimeGreaterThanEqualAndStartTimeLessThan(
                yesterdayStart,
                todayStart
        );

        AdminDashboardOverviewResponseDTO overview = new AdminDashboardOverviewResponseDTO(
                toMetric(totalUsers, usersCreatedThisMonth, usersCreatedPreviousMonth),
                toMetric(activeMeetings, activeMeetingsToday, activeMeetingsYesterday),
                toMetric(meetingsToday, meetingsToday, meetingsYesterday)
        );

        return new ApiResponse<>(overview, "Fetched dashboard overview successfully", null, HttpStatus.OK.value());
    }

    private AdminDashboardOverviewResponseDTO.MetricDTO toMetric(long value, long currentPeriod, long previousPeriod) {
        BigDecimal changePercent = calculateChangePercent(currentPeriod, previousPeriod);
        return new AdminDashboardOverviewResponseDTO.MetricDTO(
                value,
                changePercent,
                formatChange(changePercent),
                changePercent.signum() < 0 ? "down" : "up"
        );
    }

    private BigDecimal calculateChangePercent(long currentPeriod, long previousPeriod) {
        if (previousPeriod == 0L) {
            return currentPeriod == 0L ? ZERO : ONE_HUNDRED;
        }

        return BigDecimal.valueOf(currentPeriod - previousPeriod)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(previousPeriod), 1, RoundingMode.HALF_UP);
    }

    private String formatChange(BigDecimal changePercent) {
        String sign = changePercent.signum() >= 0 ? "+" : "";
        return sign + changePercent.toPlainString() + "%";
    }
}
