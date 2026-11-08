package com.nikoskatrakoulis.courtbookingsystem.service;

import com.nikoskatrakoulis.courtbookingsystem.entity.*;
import com.nikoskatrakoulis.courtbookingsystem.exception.*;
import com.nikoskatrakoulis.courtbookingsystem.repository.IdempotencyKeyRepository;
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
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    // @Transactional here too: a call from inside the same class bypasses the Spring proxy,
    // so the annotation on the 3-arg method would NOT apply to this call path.
    @Transactional
    public void createBooking(Long userId, Long slotId) {
        createBooking(userId, slotId, null);
    }

    /**
     * @param idempotencyKey optional client-supplied key; a repeat of the same request returns
     *                       success without creating a second booking
     * @return true if this call was a replay of an already-processed request
     */
    @Transactional
    public boolean createBooking(Long userId, Long slotId, String idempotencyKey) {

        validateKey(idempotencyKey);
        // 1st check, before anything else: a retry of an old success must replay even if the slot has since started.
        Optional<IdempotencyKey> known = findKey(userId, idempotencyKey);
        if (known.isPresent()) {
            return replay(known.get(), slotId);
        }

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

        // 2nd check, UNDER the slot lock: a concurrent duplicate that was waiting on the lock now sees
        // the key committed by the winner and replays instead of failing with "already booked".
        known = findKey(userId, idempotencyKey);
        if (known.isPresent()) {
            return replay(known.get(), slotId);
        }

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

        Booking saved = bookingRepository.save(newBooking);

        if (idempotencyKey != null) {
            IdempotencyKey record = new IdempotencyKey();
            record.setUser(user);
            record.setKeyValue(idempotencyKey);
            record.setSlot(lockedSlot);
            record.setBooking(saved);
            idempotencyKeyRepository.save(record);
        }
        return false;
    }

    private void validateKey(String key) {
        if (key != null && (key.isBlank() || key.length() > 100)) {
            throw new InvalidIdempotencyKeyException("Idempotency-Key must be 1-100 characters");
        }
    }

    private Optional<IdempotencyKey> findKey(Long userId, String key) {
        return key == null ? Optional.empty() : idempotencyKeyRepository.findByUser_IdAndKeyValue(userId, key);
    }

    private boolean replay(IdempotencyKey known, Long requestedSlotId) {
        if (!known.getSlot().getId().equals(requestedSlotId)) {
            throw new IdempotencyKeyReusedException("Idempotency-Key was already used for a different request");
        }
        return true;
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
