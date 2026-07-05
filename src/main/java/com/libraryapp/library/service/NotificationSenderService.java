package com.libraryapp.library.service;

import com.libraryapp.library.dto.NotificationDto;

public interface NotificationSenderService {

    void sendBookAvailableNotification(NotificationDto notification);
}
