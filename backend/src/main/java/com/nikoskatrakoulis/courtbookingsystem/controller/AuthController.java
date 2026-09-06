package com.nikoskatrakoulis.courtbookingsystem.controller;

import com.nikoskatrakoulis.courtbookingsystem.dto.request.LoginRequest;
import com.nikoskatrakoulis.courtbookingsystem.dto.request.RefreshTokenRequest;
import com.nikoskatrakoulis.courtbookingsystem.dto.request.RegisterRequest;
import com.nikoskatrakoulis.courtbookingsystem.dto.response.AuthResponse;
import com.nikoskatrakoulis.courtbookingsystem.dto.response.UserResponse;
import com.nikoskatrakoulis.courtbookingsystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName()
        );
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request.getEmail(), request.getPassword());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return userService.refresh(request.getRefreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        userService.logout(request.getRefreshToken());
    }
}
