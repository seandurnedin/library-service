package com.libraryapp.library.service.impl;

import com.libraryapp.library.client.DomainClient;
import com.libraryapp.library.dto.NotificationDto;
import com.libraryapp.library.service.NotificationBroadcastService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationSenderServiceImplTest {

    @Mock
    private NotificationBroadcastService notificationBroadcastService;
    @Mock
    private DomainClient domainClient;

    @InjectMocks
    private NotificationSenderServiceImpl notificationSenderService;

    @Test
    void sendBookAvailableNotification_persistsThenPublishesThePersistedVersion() {
        NotificationDto notification = NotificationDto.builder()
                .userId(1L).type("BOOK_AVAILABLE").message("Ready for pickup").bookId(10L).build();
        NotificationDto persisted = NotificationDto.builder()
                .id(99L).userId(1L).type("BOOK_AVAILABLE").message("Ready for pickup").bookId(10L).read(false).build();
        when(domainClient.createNotification(eq(1L), any(NotificationDto.class))).thenReturn(persisted);

        notificationSenderService.sendBookAvailableNotification(notification);

        verify(notificationBroadcastService).publish(argThat(n -> n.getId() != null && n.getId().equals(99L)));
    }

    @Test
    void sendBookAvailableNotification_persistenceFails_stillPublishesBestEffort() {
        NotificationDto notification = NotificationDto.builder()
                .userId(1L).type("BOOK_AVAILABLE").message("Ready for pickup").bookId(10L).build();
        when(domainClient.createNotification(eq(1L), any(NotificationDto.class)))
                .thenThrow(new RuntimeException("domain-service unreachable"));

        notificationSenderService.sendBookAvailableNotification(notification);

        verify(notificationBroadcastService).publish(notification);
    }

    @Test
    void sendBookAvailableNotification_persistenceReturnsNull_stillPublishesBestEffort() {
        NotificationDto notification = NotificationDto.builder()
                .userId(1L).type("BOOK_AVAILABLE").message("Ready for pickup").bookId(10L).build();
        when(domainClient.createNotification(eq(1L), any(NotificationDto.class))).thenReturn(null);

        notificationSenderService.sendBookAvailableNotification(notification);

        verify(notificationBroadcastService).publish(notification);
    }
}
