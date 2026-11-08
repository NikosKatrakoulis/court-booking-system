package com.nikoskatrakoulis.courtbookingsystem.service;

import com.nikoskatrakoulis.courtbookingsystem.dto.response.CourtOccupancyResponse;
import com.nikoskatrakoulis.courtbookingsystem.dto.response.PopularSlotResponse;
import com.nikoskatrakoulis.courtbookingsystem.entity.*;
import com.nikoskatrakoulis.courtbookingsystem.exception.InvalidDateRangeException;
import com.nikoskatrakoulis.courtbookingsystem.support.AbstractPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalyticsServiceTest extends AbstractPostgresTest {

    @Autowired
    private AnalyticsService analyticsService;

    private final LocalDate day = LocalDate.now().plusDays(3);

    private void book(User user, Slot slot, Status status) {
        Booking b = new Booking();
        b.setUser(user);
        b.setSlot(slot);
        b.setStatus(status);
        bookingRepository.save(b);
    }

    @Test
    void occupancy_countsOnlyConfirmedBookings_perCourt() {
        User u = createUser("u@example.com");
        Court busy = createCourt(true);
        Court quiet = createCourt(true);
        Slot b1 = createSlot(busy, day, LocalTime.of(10, 0));
        Slot b2 = createSlot(busy, day, LocalTime.of(11, 30));
        createSlot(busy, day, LocalTime.of(13, 0));
        createSlot(busy, day, LocalTime.of(14, 30));
        Slot q1 = createSlot(quiet, day, LocalTime.of(10, 0));
        book(u, b1, Status.CONFIRMED);
        book(u, b2, Status.CONFIRMED);
        book(u, q1, Status.CANCELLED); // cancelled must not count

        List<CourtOccupancyResponse> result = analyticsService.occupancyByCourt(day, day);

        assertThat(result).hasSize(2);
        CourtOccupancyResponse busyRow = result.stream().filter(r -> r.getCourtId().equals(busy.getId())).findFirst().orElseThrow();
        CourtOccupancyResponse quietRow = result.stream().filter(r -> r.getCourtId().equals(quiet.getId())).findFirst().orElseThrow();
        assertThat(busyRow.getTotalSlots()).isEqualTo(4);
        assertThat(busyRow.getBookedSlots()).isEqualTo(2);
        assertThat(busyRow.getOccupancyRate()).isEqualTo(0.5);
        assertThat(quietRow.getTotalSlots()).isEqualTo(1);
        assertThat(quietRow.getBookedSlots()).isZero();
        assertThat(quietRow.getOccupancyRate()).isZero();
    }

    @Test
    void occupancy_respectsDateRange() {
        Court court = createCourt(true);
        createSlot(court, day, LocalTime.of(10, 0));
        createSlot(court, day.plusDays(10), LocalTime.of(10, 0));

        assertThat(analyticsService.occupancyByCourt(day, day).get(0).getTotalSlots()).isEqualTo(1);
        assertThat(analyticsService.occupancyByCourt(day, day.plusDays(10)).get(0).getTotalSlots()).isEqualTo(2);
    }

    @Test
    void popularStartTimes_rankedByConfirmedBookings_andLimited() {
        User u = createUser("u@example.com");
        Court c1 = createCourt(true);
        Court c2 = createCourt(true);
        // 18:00 booked twice (two courts), 10:00 once, 12:00 only cancelled
        book(u, createSlot(c1, day, LocalTime.of(18, 0)), Status.CONFIRMED);
        book(u, createSlot(c2, day, LocalTime.of(18, 0)), Status.CONFIRMED);
        book(u, createSlot(c1, day, LocalTime.of(10, 0)), Status.CONFIRMED);
        book(u, createSlot(c1, day, LocalTime.of(12, 0)), Status.CANCELLED);

        List<PopularSlotResponse> top = analyticsService.popularStartTimes(day, day, 10);

        assertThat(top).extracting(PopularSlotResponse::getStartTime)
                .containsExactly(LocalTime.of(18, 0), LocalTime.of(10, 0));
        assertThat(top.get(0).getBookings()).isEqualTo(2);
        assertThat(analyticsService.popularStartTimes(day, day, 1)).hasSize(1);
    }

    @Test
    void invalidRanges_areRejected() {
        assertThatThrownBy(() -> analyticsService.occupancyByCourt(day, day.minusDays(1)))
                .isInstanceOf(InvalidDateRangeException.class);
        assertThatThrownBy(() -> analyticsService.popularStartTimes(day, day.plusDays(400), 5))
                .isInstanceOf(InvalidDateRangeException.class);
    }
}
