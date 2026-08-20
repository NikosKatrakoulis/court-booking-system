package com.nikoskatrakoulis.courtbookingsystem.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CourtUpdateRequest {

    private String name;

    private Long courtTypeId;

    private Boolean isActive;
}
