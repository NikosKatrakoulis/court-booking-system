package com.nikoskatrakoulis.courtbookingsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class BookingResponse {

    private final Long id;
    private final Long slotId;
    private final String courtName;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final BigDecimal price;
    private final String status;
    private final boolean cancellable;
    private final LocalDateTime createdAt;
    private final LocalDateTime cancelledAt;
}
