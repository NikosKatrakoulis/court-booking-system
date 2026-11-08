package com.nikoskatrakoulis.courtbookingsystem.service;

import com.nikoskatrakoulis.courtbookingsystem.entity.*;
import com.nikoskatrakoulis.courtbookingsystem.exception.*;
import com.nikoskatrakoulis.courtbookingsystem.support.AbstractPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingServiceTest extends AbstractPostgresTest {

    @Autowired
    private BookingService bookingService;

    private Slot slotStartingIn(Court court, long hours) {
        LocalDateTime start = LocalDateTime.now().plusHours(hours);
        return createSlot(court, start.toLocalDate(), start.toLocalTime().withNano(0));
    }

    private Booking confirmedBooking(User user, Slot slot) {
        Booking b = new Booking();
        b.setUser(user);
        b.setSlot(slot);
        b.setStatus(Status.CONFIRMED);
        return bookingRepository.save(b);
    }

    @Test
    void createBooking_happyPath_storesConfirmedBooking() {
        User user = createUser("u@example.com");
        Slot slot = slotStartingIn(createCourt(true), 5);

        bookingService.createBooking(user.getId(), slot.getId());

        assertThat(bookingRepository.findBySlot_IdAndStatus(slot.getId(), Status.CONFIRMED)).isPresent();
    }

    @Test
    void createBooking_pastSlot_isRejected() {
        User user = createUser("u@example.com");
        Slot slot = createSlot(createCourt(true), LocalDate.now().minusDays(1), LocalTime.of(10, 0));

        assertThatThrownBy(() -> bookingService.createBooking(user.getId(), slot.getId()))
                .isInstanceOf(SlotInThePastException.class);
    }

    @Test
    void createBooking_inactiveCourt_isRejected() {
        User user = createUser("u@example.com");
        Slot slot = slotStartingIn(createCourt(false), 5);

        assertThatThrownBy(() -> bookingService.createBooking(user.getId(), slot.getId()))
                .isInstanceOf(CourtNotAvailableException.class);
    }

    @Test
    void createBooking_unknownSlot_isRejected() {
        User user = createUser("u@example.com");

        assertThatThrownBy(() -> bookingService.createBooking(user.getId(), 999_999L))
                .isInstanceOf(SlotNotFoundException.class);
    }

    @Test
    void cancelBooking_moreThanTwoHoursAhead_succeedsAndFreesTheSlot() {
        User user = createUser("u@example.com");
        Slot slot = slotStartingIn(createCourt(true), 5);
        Booking booking = confirmedBooking(user, slot);

        bookingService.cancelBooking(user.getId(), booking.getId());

        Booking reloaded = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(Status.CANCELLED);
        assertThat(reloaded.getCancelledAt()).isNotNull();
        // the slot can be booked again (partial unique index only covers CONFIRMED rows)
        User other = createUser("other@example.com");
        bookingService.createBooking(other.getId(), slot.getId());
    }

    @Test
    void cancelBooking_lessThanTwoHoursAhead_isRejected() {
        User user = createUser("u@example.com");
        Booking booking = confirmedBooking(user, slotStartingIn(createCourt(true), 1));

        assertThatThrownBy(() -> bookingService.cancelBooking(user.getId(), booking.getId()))
                .isInstanceOf(CancellationNotAllowedException.class);
    }

    @Test
    void cancelBooking_byAnotherUser_isRejected() {
        User owner = createUser("owner@example.com");
        User stranger = createUser("stranger@example.com");
        Booking booking = confirmedBooking(owner, slotStartingIn(createCourt(true), 5));

        assertThatThrownBy(() -> bookingService.cancelBooking(stranger.getId(), booking.getId()))
                .isInstanceOf(BookingNotOwnedException.class);
    }

    @Test
    void cancelBooking_twice_isRejected() {
        User user = createUser("u@example.com");
        Booking booking = confirmedBooking(user, slotStartingIn(createCourt(true), 5));
        bookingService.cancelBooking(user.getId(), booking.getId());

        assertThatThrownBy(() -> bookingService.cancelBooking(user.getId(), booking.getId()))
                .isInstanceOf(BookingAlreadyCancelledException.class);
    }

    @Test
    void myBookings_listsOwnBookingsNewestFirst_withCancellableFlag() {
        User user = createUser("u@example.com");
        User other = createUser("other@example.com");
        Court court = createCourt(true);
        LocalDateTime farStart = LocalDateTime.now().plusHours(30);
        LocalDateTime nearStart = LocalDateTime.now().plusHours(1);
        Slot far = createSlot(court, farStart.toLocalDate(), farStart.toLocalTime().withNano(0));
        Slot near = createSlot(court, nearStart.toLocalDate(), nearStart.toLocalTime().withNano(0));
        confirmedBooking(user, far);
        confirmedBooking(user, near);
        confirmedBooking(other, createSlot(court, farStart.toLocalDate().plusDays(1), java.time.LocalTime.of(10, 0)));

        var result = bookingService.myBookings(user.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSlotId()).isEqualTo(far.getId());
        assertThat(result.get(0).isCancellable()).isTrue();
        assertThat(result.get(1).isCancellable()).isFalse();
        assertThat(result.get(0).getPrice()).isNotNull();
    }
}
