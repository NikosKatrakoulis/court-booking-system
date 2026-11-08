package com.nikoskatrakoulis.courtbookingsystem.service;

import com.nikoskatrakoulis.courtbookingsystem.entity.Court;
import com.nikoskatrakoulis.courtbookingsystem.entity.Slot;
import com.nikoskatrakoulis.courtbookingsystem.repository.CourtRepository;
import com.nikoskatrakoulis.courtbookingsystem.repository.SlotRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates concrete Slot rows for every active court over a rolling horizon.
 * Idempotent: existing slots are skipped, so it is safe to run repeatedly.
 * The unique constraint (court_id, date, start_time) is the last line of defence
 * if two instances ever run the job at the same time.
 */
@Service
public class SlotGenerationService {

    private final CourtRepository courtRepository;
    private final SlotRepository slotRepository;
    private final LocalTime openingTime;
    private final LocalTime closingTime;
    private final int horizonDays;

    public SlotGenerationService(
            CourtRepository courtRepository,
            SlotRepository slotRepository,
            @Value("${slots.opening-time}") LocalTime openingTime,
            @Value("${slots.closing-time}") LocalTime closingTime,
            @Value("${slots.horizon-days}") int horizonDays
    ) {
        if (!openingTime.isBefore(closingTime)) {
            throw new IllegalStateException("slots.opening-time must be before slots.closing-time");
        }
        if (horizonDays < 1) {
            throw new IllegalStateException("slots.horizon-days must be at least 1");
        }
        this.courtRepository = courtRepository;
        this.slotRepository = slotRepository;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.horizonDays = horizonDays;
    }

    /** @return number of newly created slots */
    @Transactional
    public int generateSlots() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate from = now.toLocalDate();
        LocalDate to = from.plusDays(horizonDays - 1L);
        int created = 0;

        for (Court court : courtRepository.findByIsActiveTrue()) {
            if (!court.getCourtType().isActive()) {
                continue;
            }
            int duration = court.getCourtType().getDurationMinutes();
            Set<String> existing = new HashSet<>();
            for (Slot s : slotRepository.findByCourt_IdAndDateBetween(court.getId(), from, to)) {
                existing.add(s.getDate() + "|" + s.getStartTime());
            }

            List<Slot> toSave = new ArrayList<>();
            for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
                LocalTime start = openingTime;
                // the isAfter(start) check guards against wrapping past midnight
                while (true) {
                    LocalTime end = start.plusMinutes(duration);
                    if (!end.isAfter(start) || end.isAfter(closingTime)) {
                        break;
                    }
                    boolean alreadyStarted = date.atTime(start).isBefore(now);
                    if (!alreadyStarted && !existing.contains(date + "|" + start)) {
                        Slot slot = new Slot();
                        slot.setCourt(court);
                        slot.setDate(date);
                        slot.setStartTime(start);
                        slot.setEndTime(end);
                        toSave.add(slot);
                    }
                    start = end;
                }
            }
            slotRepository.saveAll(toSave);
            created += toSave.size();
        }
        return created;
    }
}
