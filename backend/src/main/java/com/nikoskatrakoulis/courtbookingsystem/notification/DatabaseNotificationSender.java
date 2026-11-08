package com.nikoskatrakoulis.courtbookingsystem.notification;

import com.nikoskatrakoulis.courtbookingsystem.entity.Notification;
import com.nikoskatrakoulis.courtbookingsystem.repository.NotificationRepository;
import com.nikoskatrakoulis.courtbookingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Stores the notification so the user can read it via GET /api/notifications. */
@Component
@RequiredArgsConstructor
public class DatabaseNotificationSender implements NotificationSender {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    public void send(Long userId, String message) {
        Notification notification = new Notification();
        notification.setUser(userRepository.getReferenceById(userId));
        notification.setMessage(message);
        notificationRepository.save(notification);
    }
}
