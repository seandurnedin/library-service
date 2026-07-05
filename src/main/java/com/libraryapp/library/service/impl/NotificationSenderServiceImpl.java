package com.libraryapp.library.service.impl;

import com.libraryapp.library.client.DomainClient;
import com.libraryapp.library.dto.NotificationDto;
import com.libraryapp.library.service.NotificationBroadcastService;
import com.libraryapp.library.service.NotificationSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSenderServiceImpl implements NotificationSenderService {

    private final NotificationBroadcastService notificationBroadcastService;
    private final DomainClient domainClient;

    @Override
    public void sendBookAvailableNotification(NotificationDto notification) {

        log.info("Notifying user {} - {}", notification.getUserId(), notification.getMessage());

        NotificationDto toPublish = notification;
        try {
            NotificationDto persisted = domainClient.createNotification(notification.getUserId(), notification);
            if (persisted != null) {
                toPublish = persisted;
            }
        } catch (Exception ex) {
            log.warn("Failed to persist notification for user {}: {}", notification.getUserId(), ex.getMessage());
        }

        notificationBroadcastService.publish(toPublish);
    }
}
