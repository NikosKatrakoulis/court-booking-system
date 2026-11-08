package com.nikoskatrakoulis.courtbookingsystem.event;

import com.nikoskatrakoulis.courtbookingsystem.notification.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * - @TransactionalEventListener (AFTER_COMMIT): never notify about a change that was rolled back.
 * - @Async: runs on another thread, so a slow/failing sender cannot delay or break the cancellation.
 */
@Component
public class UserNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(UserNotificationListener.class);

    private final List<NotificationSender> senders;

    public UserNotificationListener(List<NotificationSender> senders) {
        this.senders = senders;
    }

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void on(UserNotificationEvent event) {
        for (NotificationSender sender : senders) {
            try {
                sender.send(event.userId(), event.message());
            } catch (RuntimeException e) {
                // one failing channel must not block the others
                log.error("{} failed for user {}", sender.getClass().getSimpleName(), event.userId(), e);
            }
        }
    }
}
