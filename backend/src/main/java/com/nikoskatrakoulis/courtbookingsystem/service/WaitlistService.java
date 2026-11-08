package com.nikoskatrakoulis.courtbookingsystem.service;

import com.nikoskatrakoulis.courtbookingsystem.dto.response.WaitlistEntryResponse;
import com.nikoskatrakoulis.courtbookingsystem.entity.*;
import com.nikoskatrakoulis.courtbookingsystem.event.UserNotificationEvent;
import com.nikoskatrakoulis.courtbookingsystem.exception.*;
import com.nikoskatrakoulis.courtbookingsystem.repository.BookingRepository;
import com.nikoskatrakoulis.courtbookingsystem.repository.SlotRepository;
import com.nikoskatrakoulis.courtbookingsystem.repository.UserRepository;
import com.nikoskatrakoulis.courtbookingsystem.repository.WaitlistEntryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * CONCURRENCY RULE: every operation that changes who may get a slot (book, cancel, join, leave,
 * expire an offer) first takes the PESSIMISTIC lock on the slot row, always in the same order
 * (slot first, then waitlist rows). Same lock => operations on one slot are serialized, and the
 * fixed order means no deadlocks.
 */
@Service
public class WaitlistService {

    static final Set<WaitlistStatus> ACTIVE = Set.of(WaitlistStatus.WAITING, WaitlistStatus.OFFERED);

    private final WaitlistEntryRepository waitlistRepository;
    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final int offerMinutes;

    public WaitlistService(
            WaitlistEntryRepository waitlistRepository,
            SlotRepository slotRepository,
            BookingRepository bookingRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher,
            @Value("${waitlist.offer-minutes}") int offerMinutes
    ) {
        this.waitlistRepository = waitlistRepository;
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.offerMinutes = offerMinutes;
    }

    @Transactional
    public WaitlistEntryResponse join(Long userId, Long slotId) {
        Slot slot = slotRepository.findByIdWithLock(slotId)
                .orElseThrow(() -> new SlotNotFoundException("Slot not found with id: " + slotId));

        if (slotStart(slot).isBefore(LocalDateTime.now())) {
            throw new SlotInThePastException("Cannot join the waitlist of a slot in the past");
        }

        Optional<Booking> booking = bookingRepository.findBySlot_IdAndStatus(slotId, Status.CONFIRMED);
        boolean reserved = waitlistRepository
                .findFirstBySlot_IdAndStatusAndOfferExpiresAtAfter(slotId, WaitlistStatus.OFFERED, LocalDateTime.now())
                .isPresent();

        if (booking.isEmpty() && !reserved) {
            throw new SlotStillAvailableException("Slot is available, book it directly");
        }
        if (booking.isPresent() && booking.get().getUser().getId().equals(userId)) {
            throw new SlotAlreadyBookedException("You already hold a booking for this slot");
        }
        if (waitlistRepository.findByUser_IdAndSlot_IdAndStatusIn(userId, slotId, ACTIVE).isPresent()) {
            throw new AlreadyOnWaitlistException("You are already on the waitlist for this slot");
        }

        WaitlistEntry entry = new WaitlistEntry();
        entry.setUser(userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId)));
        entry.setSlot(slot);
        entry.setStatus(WaitlistStatus.WAITING);
        return toResponse(waitlistRepository.save(entry));
    }

    @Transactional
    public void leave(Long userId, Long entryId) {
        Long slotId = waitlistRepository.findSlotIdById(entryId)
                .orElseThrow(() -> new WaitlistEntryNotFoundException("Waitlist entry not found with id: " + entryId));
        Slot slot = lockSlot(slotId);
        // Loaded only AFTER the lock, so we see the latest committed state.
        WaitlistEntry entry = waitlistRepository.findById(entryId)
                .orElseThrow(() -> new WaitlistEntryNotFoundException("Waitlist entry not found with id: " + entryId));

        if (!entry.getUser().getId().equals(userId)) {
            throw new WaitlistEntryNotOwnedException("You are not allowed to modify this waitlist entry");
        }
        if (!ACTIVE.contains(entry.getStatus())) {
            throw new WaitlistEntryNotActiveException("Waitlist entry is no longer active");
        }

        boolean hadOffer = entry.getStatus() == WaitlistStatus.OFFERED;
        entry.setStatus(WaitlistStatus.LEFT);
        waitlistRepository.save(entry);
        if (hadOffer) {
            offerToNext(slot); // pass the freed slot on
        }
    }

    @Transactional(readOnly = true)
    public List<WaitlistEntryResponse> myEntries(Long userId) {
        return waitlistRepository.findByUser_IdAndStatusInOrderByCreatedAtDesc(userId, ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<Long> findExpiredOfferIds() {
        return waitlistRepository.findIdsByStatusAndOfferExpiredBefore(WaitlistStatus.OFFERED, LocalDateTime.now());
    }

    /** One transaction per entry, so one problem never blocks the rest and locks stay short. */
    @Transactional
    public void expireOffer(Long entryId) {
        Optional<Long> slotId = waitlistRepository.findSlotIdById(entryId);
        if (slotId.isEmpty()) {
            return;
        }
        Slot slot = lockSlot(slotId.get());
        WaitlistEntry entry = waitlistRepository.findById(entryId).orElse(null);
        // Re-check under the lock: the user may have booked or left in the meantime.
        if (entry == null || entry.getStatus() != WaitlistStatus.OFFERED
                || !entry.getOfferExpiresAt().isBefore(LocalDateTime.now())) {
            return;
        }
        entry.setStatus(WaitlistStatus.EXPIRED);
        waitlistRepository.save(entry);
        notifyUser(entry.getUser().getId(), "Your offer for " + describe(slot) + " has expired.");
        offerToNext(slot);
    }

    /**
     * Offers the slot to the first WAITING user. MUST be called with the slot lock held.
     * No offer if the slot is already taken, nobody is waiting, or the slot starts before
     * the offer window would even end.
     */
    void offerToNext(Slot slot) {
        if (bookingRepository.findBySlot_IdAndStatus(slot.getId(), Status.CONFIRMED).isPresent()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(offerMinutes);
        if (slotStart(slot).isBefore(expiresAt)) {
            return;
        }
        waitlistRepository.findFirstBySlot_IdAndStatusOrderByCreatedAtAscIdAsc(slot.getId(), WaitlistStatus.WAITING)
                .ifPresent(next -> {
                    next.setStatus(WaitlistStatus.OFFERED);
                    next.setOfferExpiresAt(expiresAt);
                    waitlistRepository.save(next);
                    notifyUser(next.getUser().getId(), describe(slot) + " is now available for you. Book it within "
                            + offerMinutes + " minutes (POST /api/bookings) or the offer passes to the next person.");
                });
    }

    /**
     * Called by BookingService.createBooking (slot lock held): enforces the reservation and
     * closes the booking user's own waitlist entry. Throws if the slot is reserved for someone else.
     */
    void onBookingAttempt(Long userId, Long slotId) {
        Optional<WaitlistEntry> offer = waitlistRepository
                .findFirstBySlot_IdAndStatusAndOfferExpiresAtAfter(slotId, WaitlistStatus.OFFERED, LocalDateTime.now());
        if (offer.isPresent() && !offer.get().getUser().getId().equals(userId)) {
            throw new SlotReservedException("Slot is temporarily reserved for a user from the waitlist");
        }
        waitlistRepository.findByUser_IdAndSlot_IdAndStatusIn(userId, slotId, ACTIVE).ifPresent(own -> {
            own.setStatus(WaitlistStatus.FULFILLED);
            waitlistRepository.save(own);
        });
    }

    private Slot lockSlot(Long slotId) {
        return slotRepository.findByIdWithLock(slotId)
                .orElseThrow(() -> new SlotNotFoundException("Slot not found with id: " + slotId));
    }

    private void notifyUser(Long userId, String message) {
        eventPublisher.publishEvent(new UserNotificationEvent(userId, message));
    }

    private LocalDateTime slotStart(Slot slot) {
        return LocalDateTime.of(slot.getDate(), slot.getStartTime());
    }

    private String describe(Slot slot) {
        return slot.getCourt().getName() + " on " + slot.getDate() + " at " + slot.getStartTime();
    }

    private WaitlistEntryResponse toResponse(WaitlistEntry e) {
        Slot s = e.getSlot();
        return new WaitlistEntryResponse(e.getId(), s.getId(), s.getCourt().getName(), s.getDate(),
                s.getStartTime(), e.getStatus().name(), e.getOfferExpiresAt());
    }
}
