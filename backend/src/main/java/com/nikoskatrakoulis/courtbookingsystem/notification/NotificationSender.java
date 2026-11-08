package com.nikoskatrakoulis.courtbookingsystem.notification;

/**
 * Strategy interface: HOW a user gets notified. WaitlistService does not know or care;
 * to add email/SMS/push, add another @Component implementing this (Open/Closed Principle),
 * and the listener picks it up automatically (Dependency Inversion: it depends on this interface).
 */
public interface NotificationSender {
    void send(Long userId, String message);
}
