package com.meet.BE.service.admin;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.enums.MeetingStatus;
import com.meet.BE.domain.response.AdminDashboardOverviewResponseDTO;
import com.meet.BE.repository.MeetingRepository;
import com.meet.BE.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Test
    void shouldBuildDashboardOverviewMetrics() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();

        LocalDate firstDayOfCurrentMonth = today.withDayOfMonth(1);
        LocalDateTime currentMonthStart = firstDayOfCurrentMonth.atStartOfDay();
        LocalDateTime nextMonthStart = firstDayOfCurrentMonth.plusMonths(1).atStartOfDay();
        LocalDateTime previousMonthStart = firstDayOfCurrentMonth.minusMonths(1).atStartOfDay();

        when(userRepository.countByIsDeletedFalse()).thenReturn(2543L);
        when(userRepository.countByIsDeletedFalseAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                currentMonthStart,
                nextMonthStart
        )).thenReturn(45L);
        when(userRepository.countByIsDeletedFalseAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                previousMonthStart,
                currentMonthStart
        )).thenReturn(40L);

        when(meetingRepository.countByStatus(MeetingStatus.ACTIVE)).thenReturn(128L);
        when(meetingRepository.countByStatusAndStartTimeGreaterThanEqualAndStartTimeLessThan(
                MeetingStatus.ACTIVE,
                todayStart,
                tomorrowStart
        )).thenReturn(16L);
        when(meetingRepository.countByStatusAndStartTimeGreaterThanEqualAndStartTimeLessThan(
                MeetingStatus.ACTIVE,
                yesterdayStart,
                todayStart
        )).thenReturn(13L);

        when(meetingRepository.countByStartTimeGreaterThanEqualAndStartTimeLessThan(
                todayStart,
                tomorrowStart
        )).thenReturn(847L);
        when(meetingRepository.countByStartTimeGreaterThanEqualAndStartTimeLessThan(
                yesterdayStart,
                todayStart
        )).thenReturn(894L);

        ApiResponse<AdminDashboardOverviewResponseDTO> response = adminDashboardService.getOverview();

        assertThat(response.message()).isEqualTo("Fetched dashboard overview successfully");
        assertThat(response.status()).isEqualTo(200);

        assertThat(response.data().getTotalUsers().getValue()).isEqualTo(2543L);
        assertThat(response.data().getTotalUsers().getChangePercent()).isEqualByComparingTo(new BigDecimal("12.5"));
        assertThat(response.data().getTotalUsers().getChange()).isEqualTo("+12.5%");
        assertThat(response.data().getTotalUsers().getTrend()).isEqualTo("up");

        assertThat(response.data().getActiveMeetings().getValue()).isEqualTo(128L);
        assertThat(response.data().getActiveMeetings().getChangePercent()).isEqualByComparingTo(new BigDecimal("23.1"));
        assertThat(response.data().getActiveMeetings().getChange()).isEqualTo("+23.1%");
        assertThat(response.data().getActiveMeetings().getTrend()).isEqualTo("up");

        assertThat(response.data().getTotalMeetingsToday().getValue()).isEqualTo(847L);
        assertThat(response.data().getTotalMeetingsToday().getChangePercent()).isEqualByComparingTo(new BigDecimal("-5.3"));
        assertThat(response.data().getTotalMeetingsToday().getChange()).isEqualTo("-5.3%");
        assertThat(response.data().getTotalMeetingsToday().getTrend()).isEqualTo("down");
    }
}
