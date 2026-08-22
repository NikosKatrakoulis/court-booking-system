package com.nikoskatrakoulis.courtbookingsystem.dto.response;



import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@AllArgsConstructor
public class ValidationErrorResponse {

    private final LocalDateTime timestamp;
    private final int status;
    private final Map<String, String> errors;
}
