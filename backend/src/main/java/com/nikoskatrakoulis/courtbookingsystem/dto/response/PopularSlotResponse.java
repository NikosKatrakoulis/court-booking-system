package com.nikoskatrakoulis.courtbookingsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class PopularSlotResponse {

    private final LocalTime startTime;
    private final long bookings;
}
