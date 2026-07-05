package com.libraryapp.library.service;

import com.libraryapp.library.dto.NotificationDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationBroadcastService {

    SseEmitter subscribe(Long userId);

    void publish(NotificationDto notification);
}
