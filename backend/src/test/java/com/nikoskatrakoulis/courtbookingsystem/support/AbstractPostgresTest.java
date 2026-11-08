package com.nikoskatrakoulis.courtbookingsystem.support;

import com.nikoskatrakoulis.courtbookingsystem.entity.*;
import com.nikoskatrakoulis.courtbookingsystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Base class for integration tests against a REAL PostgreSQL (in Docker via Testcontainers).
 * Why not H2? Locking semantics (SELECT ... FOR UPDATE), partial unique indexes and the
 * Flyway migration are PostgreSQL-specific; H2 would give false confidence.
 * The container is a singleton shared by all test classes (started once per JVM).
 * The credentials below belong to a throwaway container, they are not real secrets.
 */
@SpringBootTest(properties = {
        "jwt.secret=test-only-secret-test-only-secret-test-only-secret",
        "admin.seed.email=test-admin@example.com",
        "admin.seed.password=test-only-password",
        "slots.generation-cron=-"   // disable the cron job during tests
})
public abstract class AbstractPostgresTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired protected BookingRepository bookingRepository;
    @Autowired protected SlotRepository slotRepository;
    @Autowired protected CourtRepository courtRepository;
    @Autowired protected CourtTypeRepository courtTypeRepository;
    @Autowired protected UserRepository userRepository;
    @Autowired protected RefreshTokenRepository refreshTokenRepository;

    /** Tests commit real transactions (needed for multi-thread tests), so we clean up by hand. */
    @BeforeEach
    void cleanDatabase() {
        bookingRepository.deleteAllInBatch();
        refreshTokenRepository.deleteAllInBatch();
        slotRepository.deleteAllInBatch();
        courtRepository.deleteAllInBatch();
        courtTypeRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    protected Court createCourt(boolean active) {
        CourtType type = new CourtType();
        type.setName("Padel-" + System.nanoTime());
        type.setDurationMinutes(90);
        type.setPricePerSlot(new BigDecimal("24.00"));
        type = courtTypeRepository.save(type);

        Court court = new Court();
        court.setName("Court-" + System.nanoTime());
        court.setCourtType(type);
        court.setActive(active);
        return courtRepository.save(court);
    }

    protected Slot createSlot(Court court, LocalDate date, LocalTime start) {
        Slot slot = new Slot();
        slot.setCourt(court);
        slot.setDate(date);
        slot.setStartTime(start);
        slot.setEndTime(start.plusMinutes(90));
        return slotRepository.save(slot);
    }

    protected User createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPassword("not-a-real-hash");
        user.setRole(Role.USER);
        return userRepository.save(user);
    }
}
