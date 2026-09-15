package com.nikoskatrakoulis.courtbookingsystem.dto.request;


import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BookingCreateRequest {

    @NotNull(message = "Slot id is required")
    private Long slotId;
}
