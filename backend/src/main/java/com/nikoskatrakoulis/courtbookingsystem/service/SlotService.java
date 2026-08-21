package com.nikoskatrakoulis.courtbookingsystem.service;

import com.nikoskatrakoulis.courtbookingsystem.dto.response.SlotResponse;
import com.nikoskatrakoulis.courtbookingsystem.repository.SlotRepository;
import com.nikoskatrakoulis.courtbookingsystem.repository.projection.SlotAvailabilityProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotService {

    private final SlotRepository slotRepository;

    public List<SlotResponse> getAvailableSlots(Long courtId, LocalDate date) {
        List<SlotAvailabilityProjection> projections = slotRepository.findAvailableSlots(courtId, date);
        return projections.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public SlotResponse toResponse(SlotAvailabilityProjection projection) {
        boolean isAvailable = (projection.getBookingId() == null);
        return new SlotResponse(projection.getSlotId(), projection.getStartTime(), projection.getEndTime(), isAvailable);
    }
}
