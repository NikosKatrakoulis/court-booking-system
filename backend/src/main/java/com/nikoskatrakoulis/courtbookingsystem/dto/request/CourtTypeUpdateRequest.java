package com.nikoskatrakoulis.courtbookingsystem.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class CourtTypeUpdateRequest {

    private String name;

    private Integer durationMinutes;

    private BigDecimal pricePerSlot;

    private Boolean isActive;
}
