package com.nikoskatrakoulis.courtbookingsystem.service;

import com.nikoskatrakoulis.courtbookingsystem.entity.*;
import com.nikoskatrakoulis.courtbookingsystem.exception.*;
import com.nikoskatrakoulis.courtbookingsystem.support.AbstractPostgresTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

class WaitlistServiceTest extends AbstractPostgresTest {

    @Autowired private WaitlistService waitlistService;
    @Autowired private BookingService bookingService;

    private User holder, first, second, third;
    private Slot slot;
    private Booking holderBooking;

    @BeforeEach
    void setUp() {
        holder = createUser("holder@example.com");
        first = createUser("first@example.com");
        second = createUser("second@example.com");
        third = createUser("third@example.com");
        LocalDateTime start = LocalDateTime.now().plusHours(5);
        slot = createSlot(createCourt(true), start.toLocalDate(), start.toLocalTime().withNano(0));
        holderBooking = new Booking();
        holderBooking.setUser(holder);
        holderBooking.setSlot(slot);
        holderBooking.setStatus(Status.CONFIRMED);
        holderBooking = bookingRepository.save(holderBooking);
    }

    private WaitlistStatus statusOf(User user) {
        return waitlistEntryRepository.findAll().stream()
                .filter(e -> e.getUser().getId().equals(user.getId()))
                .findFirst().orElseThrow().getStatus();
    }

    @Test
    void join_whenSlotIsAvailable_isRejected() {
        bookingService.cancelBooking(holder.getId(), holderBooking.getId());

        assertThatThrownBy(() -> waitlistService.join(first.getId(), slot.getId()))
                .isInstanceOf(SlotStillAvailableException.class);
    }

    @Test
    void join_twice_orByTheHolder_isRejected() {
        waitlistService.join(first.getId(), slot.getId());

        assertThatThrownBy(() -> waitlistService.join(first.getId(), slot.getId()))
                .isInstanceOf(AlreadyOnWaitlistException.class);
        assertThatThrownBy(() -> waitlistService.join(holder.getId(), slot.getId()))
                .isInstanceOf(SlotAlreadyBookedException.class);
    }

    @Test
    void cancel_offersSlotToFirstInLine_andNotifiesThemAsynchronously() {
        waitlistService.join(first.getId(), slot.getId());
        waitlistService.join(second.getId(), slot.getId());

        bookingService.cancelBooking(holder.getId(), holderBooking.getId());

        assertThat(statusOf(first)).isEqualTo(WaitlistStatus.OFFERED);   // FIFO
        assertThat(statusOf(second)).isEqualTo(WaitlistStatus.WAITING);
        // notification is written by the async listener after commit -> wait for it
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(notificationRepository.findByUser_IdOrderByCreatedAtDescIdDesc(first.getId())).hasSize(1));
        assertThat(notificationRepository.findByUser_IdOrderByCreatedAtDescIdDesc(second.getId())).isEmpty();
    }

    @Test
    void whileOffered_onlyTheOffereeCanBook_andTheirEntryBecomesFulfilled() {
        waitlistService.join(first.getId(), slot.getId());
        bookingService.cancelBooking(holder.getId(), holderBooking.getId());

        assertThatThrownBy(() -> bookingService.createBooking(third.getId(), slot.getId()))
                .isInstanceOf(SlotReservedException.class);

        bookingService.createBooking(first.getId(), slot.getId());
        assertThat(statusOf(first)).isEqualTo(WaitlistStatus.FULFILLED);
    }

    @Test
    void expiredOffer_passesToNextInLine() {
        waitlistService.join(first.getId(), slot.getId());
        waitlistService.join(second.getId(), slot.getId());
        bookingService.cancelBooking(holder.getId(), holderBooking.getId());
        WaitlistEntry offered = waitlistEntryRepository.findAll().stream()
                .filter(e -> e.getStatus() == WaitlistStatus.OFFERED).findFirst().orElseThrow();
        offered.setOfferExpiresAt(LocalDateTime.now().minusMinutes(1)); // simulate the 30 minutes passing
        waitlistEntryRepository.save(offered);

        List<Long> expired = waitlistService.findExpiredOfferIds();
        assertThat(expired).containsExactly(offered.getId());
        waitlistService.expireOffer(offered.getId());

        assertThat(statusOf(first)).isEqualTo(WaitlistStatus.EXPIRED);
        assertThat(statusOf(second)).isEqualTo(WaitlistStatus.OFFERED);
    }

    @Test
    void expiredOffer_whenNobodyElseWaits_slotBecomesBookableByAnyone() {
        waitlistService.join(first.getId(), slot.getId());
        bookingService.cancelBooking(holder.getId(), holderBooking.getId());
        WaitlistEntry offered = waitlistEntryRepository.findAll().get(0);
        offered.setOfferExpiresAt(LocalDateTime.now().minusMinutes(1));
        waitlistEntryRepository.save(offered);

        // even before the job runs, an expired offer no longer blocks other users
        bookingService.createBooking(third.getId(), slot.getId());

        assertThat(bookingRepository.findBySlot_IdAndStatus(slot.getId(), Status.CONFIRMED)).isPresent();
        // and when the job finally runs it must NOT re-offer a slot that is already taken
        waitlistService.expireOffer(offered.getId());
        assertThat(statusOf(first)).isEqualTo(WaitlistStatus.EXPIRED);
    }

    @Test
    void leavingWhileOffered_passesTheOfferOn_andOnlyOwnerMayLeave() {
        waitlistService.join(first.getId(), slot.getId());
        waitlistService.join(second.getId(), slot.getId());
        bookingService.cancelBooking(holder.getId(), holderBooking.getId());
        Long firstEntryId = waitlistEntryRepository.findAll().stream()
                .filter(e -> e.getUser().getId().equals(first.getId())).findFirst().orElseThrow().getId();

        assertThatThrownBy(() -> waitlistService.leave(third.getId(), firstEntryId))
                .isInstanceOf(WaitlistEntryNotOwnedException.class);

        waitlistService.leave(first.getId(), firstEntryId);

        assertThat(statusOf(first)).isEqualTo(WaitlistStatus.LEFT);
        assertThat(statusOf(second)).isEqualTo(WaitlistStatus.OFFERED);
        assertThatThrownBy(() -> waitlistService.leave(first.getId(), firstEntryId))
                .isInstanceOf(WaitlistEntryNotActiveException.class);
    }

    @Test
    void reservedSlot_raceBetweenOffereeAndOutsiders_onlyOffereeWins() throws Exception {
        waitlistService.join(first.getId(), slot.getId());
        bookingService.cancelBooking(holder.getId(), holderBooking.getId());
        List<User> contenders = new ArrayList<>(List.of(first));
        for (int i = 0; i < 5; i++) {
            contenders.add(createUser("outsider" + i + "@example.com"));
        }

        ExecutorService pool = Executors.newFixedThreadPool(contenders.size());
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger winners = new AtomicInteger();
        AtomicInteger reserved = new AtomicInteger();
        List<Throwable> unexpected = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>();
        for (User u : contenders) {
            futures.add(pool.submit(() -> {
                try {
                    go.await();
                    bookingService.createBooking(u.getId(), slot.getId());
                    assertThat(u.getId()).isEqualTo(first.getId()); // only the offeree may win
                    winners.incrementAndGet();
                } catch (SlotReservedException | SlotAlreadyBookedException e) {
                    reserved.incrementAndGet();
                } catch (Throwable t) {
                    unexpected.add(t);
                }
            }));
        }
        go.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(unexpected).isEmpty();
        assertThat(winners.get()).isEqualTo(1);
        assertThat(reserved.get()).isEqualTo(5);
    }
}
