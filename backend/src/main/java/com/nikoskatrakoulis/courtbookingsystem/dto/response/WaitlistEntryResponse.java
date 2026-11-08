package com.nikoskatrakoulis.courtbookingsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class WaitlistEntryResponse {

    private final Long id;
    private final Long slotId;
    private final String courtName;
    private final LocalDate date;
    private final LocalTime startTime;
    private final String status;
    private final LocalDateTime offerExpiresAt;
}
