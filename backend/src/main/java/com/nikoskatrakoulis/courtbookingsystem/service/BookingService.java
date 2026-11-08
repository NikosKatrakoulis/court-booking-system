package com.nikoskatrakoulis.courtbookingsystem.service;

import com.nikoskatrakoulis.courtbookingsystem.entity.*;
import com.nikoskatrakoulis.courtbookingsystem.exception.*;
import com.nikoskatrakoulis.courtbookingsystem.repository.BookingRepository;
import com.nikoskatrakoulis.courtbookingsystem.repository.CourtRepository;
import com.nikoskatrakoulis.courtbookingsystem.repository.SlotRepository;
import com.nikoskatrakoulis.courtbookingsystem.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final WaitlistService waitlistService;

    @Transactional
    public void createBooking(Long userId, Long slotId) {

        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new SlotNotFoundException("Slot not found with id: " + slotId));


        LocalDateTime slotDateTime = LocalDateTime.of(slot.getDate(), slot.getStartTime());
        if (slotDateTime.isBefore(LocalDateTime.now())) {
            throw new SlotInThePastException("Cannot book a slot in the past");
        }

        if (!slot.getCourt().isActive()) {
            throw new CourtNotAvailableException("Court is not available for booking");
        }

        if (!slot.getCourt().getCourtType().isActive()) {
            throw new CourtTypeNotAvailableException("Court type is not available for booking");
        }

        Slot lockedSlot = slotRepository.findByIdWithLock(slotId)
                .orElseThrow(() -> new SlotNotFoundException("Slot not found with id: " + slotId));

        Optional<Booking> existingBooking = bookingRepository.findBySlot_IdAndStatus(slotId, Status.CONFIRMED);
        if (existingBooking.isPresent()) {
            throw new SlotAlreadyBookedException("Slot is already booked");
        }

        // slot is free of bookings, but may be reserved for a waitlisted user (throws if so)
        waitlistService.onBookingAttempt(userId, slotId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        Booking newBooking = new Booking();
        newBooking.setUser(user);
        newBooking.setSlot(lockedSlot);
        newBooking.setStatus(Status.CONFIRMED);

        bookingRepository.save(newBooking);
    }

    @Transactional
    public void cancelBooking(Long userId, Long bookingId) {

        Long slotId = bookingRepository.findSlotIdById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + bookingId));
        // Lock the slot FIRST (same order as createBooking/waitlist), then read the booking, so we
        // see its latest state and two concurrent cancels cannot both succeed.
        Slot lockedSlot = slotRepository.findByIdWithLock(slotId)
                .orElseThrow(() -> new SlotNotFoundException("Slot not found with id: " + slotId));
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getUser().getId().equals(userId)) {
            throw new BookingNotOwnedException("You are not allowed to cancel this booking");
        }

        if (booking.getStatus() == Status.CANCELLED) {
            throw new BookingAlreadyCancelledException("Booking is already cancelled");
        }

        LocalDateTime slotDateTime = LocalDateTime.of(
                booking.getSlot().getDate(),
                booking.getSlot().getStartTime()
        );

        Duration timeUntilSlot = Duration.between(LocalDateTime.now(), slotDateTime);
        Duration minimumNotice = Duration.ofHours(2);

        if (timeUntilSlot.compareTo(minimumNotice) < 0) {
            throw new CancellationNotAllowedException("Cannot cancel less than 2 hours before");
        }

        booking.setStatus(Status.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());

        bookingRepository.save(booking);

        // the slot is free again: offer it to the first person on the waitlist
        waitlistService.offerToNext(lockedSlot);
    }
}
