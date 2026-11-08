package com.nikoskatrakoulis.courtbookingsystem.service;

import com.nikoskatrakoulis.courtbookingsystem.dto.response.CourtOccupancyResponse;
import com.nikoskatrakoulis.courtbookingsystem.dto.response.PopularSlotResponse;
import com.nikoskatrakoulis.courtbookingsystem.exception.InvalidDateRangeException;
import com.nikoskatrakoulis.courtbookingsystem.repository.BookingRepository;
import com.nikoskatrakoulis.courtbookingsystem.repository.SlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    static final int MAX_RANGE_DAYS = 366;
    static final int MAX_LIMIT = 50;

    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public List<CourtOccupancyResponse> occupancyByCourt(LocalDate from, LocalDate to) {
        validate(from, to);
        return slotRepository.findOccupancy(from, to).stream()
                .map(r -> new CourtOccupancyResponse(
                        r.getCourtId(), r.getCourtName(), r.getTotalSlots(), r.getBookedSlots(),
                        r.getTotalSlots() == 0 ? 0.0
                                : Math.round(10_000.0 * r.getBookedSlots() / r.getTotalSlots()) / 10_000.0))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PopularSlotResponse> popularStartTimes(LocalDate from, LocalDate to, int limit) {
        validate(from, to);
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        return bookingRepository.findPopularStartTimes(from, to, PageRequest.of(0, safeLimit)).stream()
                .map(r -> new PopularSlotResponse(r.getStartTime(), r.getBookings()))
                .toList();
    }

    private void validate(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new InvalidDateRangeException("'from' must not be after 'to'");
        }
        if (from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
            throw new InvalidDateRangeException("Date range must not exceed " + MAX_RANGE_DAYS + " days");
        }
    }
}
