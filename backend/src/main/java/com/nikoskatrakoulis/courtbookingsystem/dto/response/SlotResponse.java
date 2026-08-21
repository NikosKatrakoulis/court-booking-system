package com.nikoskatrakoulis.courtbookingsystem.dto.response;

import com.nikoskatrakoulis.courtbookingsystem.entity.Booking;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class SlotResponse {

    private final Long id;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Boolean available;
}
