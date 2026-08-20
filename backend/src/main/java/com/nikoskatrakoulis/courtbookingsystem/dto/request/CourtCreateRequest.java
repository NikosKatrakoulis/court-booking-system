package com.nikoskatrakoulis.courtbookingsystem.dto.request;

import com.nikoskatrakoulis.courtbookingsystem.entity.CourtType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CourtCreateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Court type is required")
    private Long courtTypeId;

    private Boolean isActive;
}
