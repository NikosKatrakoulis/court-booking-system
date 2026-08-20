package com.nikoskatrakoulis.courtbookingsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class CourtTypeCreateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Duration is required")
    private Integer durationMinutes;

    @NotNull(message = "Price is required")
    private BigDecimal pricePerSlot;

    private Boolean isActive;
}
