package com.nikoskatrakoulis.courtbookingsystem.service;

import com.nikoskatrakoulis.courtbookingsystem.entity.*;
import com.nikoskatrakoulis.courtbookingsystem.exception.SlotAlreadyBookedException;
import com.nikoskatrakoulis.courtbookingsystem.support.AbstractPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The core claim of the project: pessimistic locking (SELECT ... FOR UPDATE on the slot row)
 * prevents double-booking when many users book the same slot at the same moment.
 */
class BookingConcurrencyTest extends AbstractPostgresTest {

    private static final int THREADS = 8;

    @Autowired
    private BookingService bookingService;

    @Test
    void manyUsersBookingSameSlotSimultaneously_exactlyOneSucceeds() throws Exception {
        Slot slot = createSlot(createCourt(true), LocalDate.now().plusDays(1), LocalTime.of(10, 0));
        List<User> users = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            users.add(createUser("racer" + i + "@example.com"));
        }

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS); // every thread is warmed up and waiting
        CountDownLatch go = new CountDownLatch(1);          // the starting gun
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejectedAsBooked = new AtomicInteger();
        List<Throwable> unexpected = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        for (User user : users) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    bookingService.createBooking(user.getId(), slot.getId());
                    succeeded.incrementAndGet();
                } catch (SlotAlreadyBookedException e) {
                    rejectedAsBooked.incrementAndGet();
                } catch (Throwable t) {
                    unexpected.add(t);
                }
            }));
        }

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        go.countDown(); // release all threads at once
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(unexpected).isEmpty();
        assertThat(succeeded.get()).isEqualTo(1);
        assertThat(rejectedAsBooked.get()).isEqualTo(THREADS - 1);
        assertThat(bookingRepository.findBySlot_IdAndStatus(slot.getId(), Status.CONFIRMED)).isPresent();
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    void databaseItselfRejectsSecondConfirmedBooking_evenIfServiceLogicIsBypassed() {
        Slot slot = createSlot(createCourt(true), LocalDate.now().plusDays(1), LocalTime.of(10, 0));
        User a = createUser("a@example.com");
        User b = createUser("b@example.com");
        bookingRepository.saveAndFlush(confirmed(a, slot));

        // Goes straight to the repository, skipping BookingService and its lock.
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> bookingRepository.saveAndFlush(confirmed(b, slot)));
    }

    private Booking confirmed(User user, Slot slot) {
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setSlot(slot);
        booking.setStatus(Status.CONFIRMED);
        return booking;
    }
}
