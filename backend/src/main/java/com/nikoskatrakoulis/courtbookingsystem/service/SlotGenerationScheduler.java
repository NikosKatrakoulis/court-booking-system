package com.nikoskatrakoulis.courtbookingsystem.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Triggers slot generation on startup and then on a cron schedule (default: daily 01:00). */
@Component
@RequiredArgsConstructor
public class SlotGenerationScheduler {

    private static final Logger log = LoggerFactory.getLogger(SlotGenerationScheduler.class);

    private final SlotGenerationService slotGenerationService;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        run("startup");
    }

    @Scheduled(cron = "${slots.generation-cron}")
    public void scheduled() {
        run("scheduled");
    }

    private void run(String trigger) {
        try {
            int created = slotGenerationService.generateSlots();
            log.info("Slot generation ({}) created {} new slots", trigger, created);
        } catch (RuntimeException e) {
            // a failed run must not crash the app or kill the schedule; the next run retries
            log.error("Slot generation ({}) failed", trigger, e);
        }
    }
}
