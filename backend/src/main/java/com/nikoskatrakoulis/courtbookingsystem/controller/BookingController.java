package com.nikoskatrakoulis.courtbookingsystem.controller;

import com.nikoskatrakoulis.courtbookingsystem.dto.request.BookingCreateRequest;
import com.nikoskatrakoulis.courtbookingsystem.entity.User;
import com.nikoskatrakoulis.courtbookingsystem.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createBooking(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BookingCreateRequest request
    ) {
        bookingService.createBooking(user.getId(), request.getSlotId());
    }

    @PatchMapping("/{id}")
    public void cancelBooking(
            @AuthenticationPrincipal User user,
            @PathVariable(name = "id") Long bookingId
    ) {
        bookingService.cancelBooking(user.getId(),bookingId);
    }
}
