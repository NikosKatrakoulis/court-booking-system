package com.nikoskatrakoulis.courtbookingsystem.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically expires waitlist offers that were not taken in time and passes them on. */
@Component
@RequiredArgsConstructor
public class WaitlistExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(WaitlistExpiryScheduler.class);

    private final WaitlistService waitlistService;

    @Scheduled(cron = "${waitlist.expiry-check-cron}")
    public void expireOffers() {
        for (Long id : waitlistService.findExpiredOfferIds()) {
            try {
                waitlistService.expireOffer(id);
            } catch (RuntimeException e) {
                log.error("Failed to expire waitlist offer {}", id, e);
            }
        }
    }
}
