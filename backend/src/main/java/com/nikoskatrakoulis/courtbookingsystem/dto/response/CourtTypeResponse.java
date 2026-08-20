package com.nikoskatrakoulis.courtbookingsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CourtTypeResponse {

    private final Long id;

    private final String name;

    private final Integer durationMinutes;

    private final BigDecimal pricePerSlot;

    private final Boolean isActive;
}
