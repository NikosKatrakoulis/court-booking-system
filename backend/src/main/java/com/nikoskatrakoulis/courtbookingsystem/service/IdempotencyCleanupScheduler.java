package com.nikoskatrakoulis.courtbookingsystem.service;

import com.nikoskatrakoulis.courtbookingsystem.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Keys only need to live as long as a client might realistically retry; this keeps the table small. */
@Component
@RequiredArgsConstructor
public class IdempotencyCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupScheduler.class);

    private final IdempotencyKeyRepository repository;

    @Value("${idempotency.retention-hours}")
    private int retentionHours;

    @Scheduled(cron = "${idempotency.cleanup-cron}")
    @Transactional
    public void cleanup() {
        int deleted = repository.deleteOlderThan(LocalDateTime.now().minusHours(retentionHours));
        if (deleted > 0) {
            log.info("Deleted {} expired idempotency keys", deleted);
        }
    }
}
