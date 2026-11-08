package com.nikoskatrakoulis.courtbookingsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CourtOccupancyResponse {

    private final Long courtId;
    private final String courtName;
    private final long totalSlots;
    private final long bookedSlots;
    /** bookedSlots / totalSlots, between 0.0 and 1.0 */
    private final double occupancyRate;
}
