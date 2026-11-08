package com.nikoskatrakoulis.courtbookingsystem.controller;

import com.nikoskatrakoulis.courtbookingsystem.dto.request.BookingCreateRequest;
import com.nikoskatrakoulis.courtbookingsystem.dto.response.BookingResponse;
import com.nikoskatrakoulis.courtbookingsystem.entity.User;
import com.nikoskatrakoulis.courtbookingsystem.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /**
     * Send an "Idempotency-Key" header (e.g. a UUID generated once per user action) to make retries
     * and double-clicks safe: the same key + same slot never creates a second booking.
     * A replay answers 201 too, flagged with the response header "Idempotent-Replayed: true".
     */
    @PostMapping
    public ResponseEntity<Void> createBooking(
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody BookingCreateRequest request
    ) {
        boolean replayed = bookingService.createBooking(user.getId(), request.getSlotId(), idempotencyKey);
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.CREATED);
        if (replayed) {
            response.header("Idempotent-Replayed", "true");
        }
        return response.build();
    }

    @GetMapping
    public List<BookingResponse> myBookings(@AuthenticationPrincipal User user) {
        return bookingService.myBookings(user.getId());
    }

    @PatchMapping("/{id}")
    public void cancelBooking(
            @AuthenticationPrincipal User user,
            @PathVariable(name = "id") Long bookingId
    ) {
        bookingService.cancelBooking(user.getId(),bookingId);
    }
}
