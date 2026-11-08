package com.nikoskatrakoulis.courtbookingsystem.service;

import com.nikoskatrakoulis.courtbookingsystem.entity.Court;
import com.nikoskatrakoulis.courtbookingsystem.entity.Slot;
import com.nikoskatrakoulis.courtbookingsystem.support.AbstractPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SlotGenerationServiceTest extends AbstractPostgresTest {

    @Autowired
    private SlotGenerationService service;

    @Test
    void generatesSlotsForActiveCourts_andIsIdempotent() {
        Court court = createCourt(true);

        int first = service.generateSlots();
        long countAfterFirst = slotRepository.count();
        int second = service.generateSlots();

        assertThat(first).isGreaterThan(0);
        assertThat(first).isEqualTo((int) countAfterFirst);
        assertThat(second).isZero();
        assertThat(slotRepository.count()).isEqualTo(countAfterFirst);
        assertThat(court.getId()).isNotNull();
    }

    @Test
    void skipsInactiveCourts() {
        createCourt(false);

        assertThat(service.generateSlots()).isZero();
    }

    @Test
    void neverGeneratesSlotsInThePast_andRespectsOpeningHours() {
        Court court = createCourt(true);
        service.generateSlots();

        List<Slot> slots = slotRepository.findByCourt_IdAndDateBetween(
                court.getId(), LocalDate.now(), LocalDate.now().plusDays(30));

        assertThat(slots).isNotEmpty();
        LocalDateTime now = LocalDateTime.now();
        assertThat(slots).allSatisfy(s -> {
            assertThat(LocalDateTime.of(s.getDate(), s.getStartTime())).isAfterOrEqualTo(now.withNano(0).minusSeconds(1));
            assertThat(s.getStartTime()).isAfterOrEqualTo(java.time.LocalTime.of(9, 0));
            assertThat(s.getEndTime()).isBeforeOrEqualTo(java.time.LocalTime.of(23, 0));
        });
    }
}
