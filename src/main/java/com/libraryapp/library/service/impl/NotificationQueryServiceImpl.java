package com.libraryapp.library.service.impl;

import com.libraryapp.library.client.DomainClient;
import com.libraryapp.library.dto.NotificationDto;
import com.libraryapp.library.exception.ResourceNotFoundException;
import com.libraryapp.library.service.NotificationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationQueryServiceImpl implements NotificationQueryService {

    private final DomainClient domainClient;

    @Override
    public List<NotificationDto> getMyUnreadNotifications(Long userId) {
        return domainClient.getUnreadNotificationsByUser(userId);
    }

    @Override
    public NotificationDto markAsRead(Long requestingUserId, Long notificationId) {
        NotificationDto notification = domainClient.getNotificationById(notificationId);
        if (notification == null) {
            throw new ResourceNotFoundException("Notification not found: " + notificationId);
        }
        if (!notification.getUserId().equals(requestingUserId)) {
            throw new AccessDeniedException("Cannot mark another user's notification as read");
        }
        return domainClient.markNotificationRead(notificationId);
    }
}
