package com.libraryapp.library.service;

import com.libraryapp.library.dto.NotificationDto;

import java.util.List;


public interface NotificationQueryService {

    List<NotificationDto> getMyUnreadNotifications(Long userId);

    NotificationDto markAsRead(Long requestingUserId, Long notificationId);
}
