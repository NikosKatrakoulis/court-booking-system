package com.nikoskatrakoulis.courtbookingsystem.event;

/** Published inside a transaction; delivered only AFTER that transaction commits (see listener). */
public record UserNotificationEvent(Long userId, String message) {
}
