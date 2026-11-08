package com.nikoskatrakoulis.courtbookingsystem.service;

import com.nikoskatrakoulis.courtbookingsystem.entity.*;
import com.nikoskatrakoulis.courtbookingsystem.exception.*;
import com.nikoskatrakoulis.courtbookingsystem.support.AbstractPostgresTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyTest extends AbstractPostgresTest {

    @Autowired private BookingService bookingService;
    @Autowired private IdempotencyCleanupScheduler cleanupScheduler;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private User user, other;
    private Slot slot, otherSlot;

    @BeforeEach
    void setUp() {
        user = createUser("user@example.com");
        other = createUser("other@example.com");
        Court court = createCourt(true);
        LocalDate day = LocalDate.now().plusDays(2);
        slot = createSlot(court, day, LocalTime.of(10, 0));
        otherSlot = createSlot(court, day, LocalTime.of(12, 0));
    }

    @Test
    void sameKeyTwice_createsOneBooking_andSecondCallIsAReplay() {
        boolean first = bookingService.createBooking(user.getId(), slot.getId(), "key-1");
        boolean second = bookingService.createBooking(user.getId(), slot.getId(), "key-1");

        assertThat(first).isFalse();
        assertThat(second).isTrue();
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    void withoutKey_aRepeatIsStillRejectedAsAlreadyBooked() {
        bookingService.createBooking(user.getId(), slot.getId());

        assertThatThrownBy(() -> bookingService.createBooking(user.getId(), slot.getId()))
                .isInstanceOf(SlotAlreadyBookedException.class);
    }

    @Test
    void sameKeyForDifferentSlot_isRejected_andCreatesNothing() {
        bookingService.createBooking(user.getId(), slot.getId(), "key-1");

        assertThatThrownBy(() -> bookingService.createBooking(user.getId(), otherSlot.getId(), "key-1"))
                .isInstanceOf(IdempotencyKeyReusedException.class);
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    void keysAreScopedPerUser() {
        bookingService.createBooking(user.getId(), slot.getId(), "shared-key");

        // another user using the same key text must NOT be treated as a replay of someone else's request
        assertThatThrownBy(() -> bookingService.createBooking(other.getId(), slot.getId(), "shared-key"))
                .isInstanceOf(SlotAlreadyBookedException.class);
        assertThat(bookingService.createBooking(other.getId(), otherSlot.getId(), "shared-key")).isFalse();
    }

    @Test
    void failedRequest_doesNotConsumeTheKey() {
        bookingService.createBooking(other.getId(), slot.getId()); // slot taken by someone else

        assertThatThrownBy(() -> bookingService.createBooking(user.getId(), slot.getId(), "retry-key"))
                .isInstanceOf(SlotAlreadyBookedException.class);
        assertThat(idempotencyKeyRepository.count()).isZero();
        // the same key works later on another slot, because the failed attempt stored nothing
        assertThat(bookingService.createBooking(user.getId(), otherSlot.getId(), "retry-key")).isFalse();
    }

    @Test
    void invalidKey_isRejected() {
        assertThatThrownBy(() -> bookingService.createBooking(user.getId(), slot.getId(), "  "))
                .isInstanceOf(InvalidIdempotencyKeyException.class);
        assertThatThrownBy(() -> bookingService.createBooking(user.getId(), slot.getId(), "x".repeat(101)))
                .isInstanceOf(InvalidIdempotencyKeyException.class);
    }

    @Test
    void doubleClick_manyConcurrentRequestsWithSameKey_exactlyOneBooking_andNobodyGetsAnError() throws Exception {
        int threads = 6;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger created = new AtomicInteger();
        AtomicInteger replayed = new AtomicInteger();
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                try {
                    go.await();
                    if (bookingService.createBooking(user.getId(), slot.getId(), "double-click")) {
                        replayed.incrementAndGet();
                    } else {
                        created.incrementAndGet();
                    }
                } catch (Throwable t) {
                    errors.add(t);
                }
            }));
        }
        go.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(errors).isEmpty();
        assertThat(created.get()).isEqualTo(1);
        assertThat(replayed.get()).isEqualTo(threads - 1);
        assertThat(bookingRepository.count()).isEqualTo(1);
        assertThat(idempotencyKeyRepository.count()).isEqualTo(1);
    }

    @Test
    void cleanup_removesOnlyKeysOlderThanRetention() {
        bookingService.createBooking(user.getId(), slot.getId(), "old-key");
        bookingService.createBooking(user.getId(), otherSlot.getId(), "fresh-key");
        // created_at is set by Hibernate and not updatable, so age one row with plain SQL
        jdbcTemplate.update("UPDATE idempotency_key SET created_at = ? WHERE key_value = 'old-key'",
                LocalDateTime.now().minusHours(48));

        cleanupScheduler.cleanup();

        assertThat(idempotencyKeyRepository.findAll()).extracting(IdempotencyKey::getKeyValue)
                .containsExactly("fresh-key");
    }
}
