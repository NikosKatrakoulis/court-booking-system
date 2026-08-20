package com.nikoskatrakoulis.courtbookingsystem.dto.response;

import com.nikoskatrakoulis.courtbookingsystem.entity.CourtType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CourtResponse {

    private final Long id;
    private final String name;
    private final Long courtTypeId;
    private final boolean isActive;
}
