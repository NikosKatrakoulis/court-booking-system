package com.nikoskatrakoulis.courtbookingsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponse {

    private final Long id;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String role;
}
