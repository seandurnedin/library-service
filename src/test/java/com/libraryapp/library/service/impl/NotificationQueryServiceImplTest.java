package com.libraryapp.library.service.impl;

import com.libraryapp.library.client.DomainClient;
import com.libraryapp.library.dto.NotificationDto;
import com.libraryapp.library.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceImplTest {

    @Mock
    private DomainClient domainClient;

    @InjectMocks
    private NotificationQueryServiceImpl notificationQueryService;

    @Test
    void getMyUnreadNotifications_delegatesToDomainClient() {
        NotificationDto notification = NotificationDto.builder().id(1L).userId(5L).build();
        when(domainClient.getUnreadNotificationsByUser(5L)).thenReturn(List.of(notification));

        List<NotificationDto> result = notificationQueryService.getMyUnreadNotifications(5L);

        assertThat(result).containsExactly(notification);
    }

    @Test
    void markAsRead_ownNotification_marksItRead() {
        NotificationDto notification = NotificationDto.builder().id(1L).userId(5L).read(false).build();
        NotificationDto updated = NotificationDto.builder().id(1L).userId(5L).read(true).build();
        when(domainClient.getNotificationById(1L)).thenReturn(notification);
        when(domainClient.markNotificationRead(1L)).thenReturn(updated);

        NotificationDto result = notificationQueryService.markAsRead(5L, 1L);

        assertThat(result.getRead()).isTrue();
    }

    @Test
    void markAsRead_notFound_throws() {
        when(domainClient.getNotificationById(99L)).thenReturn(null);

        assertThatThrownBy(() -> notificationQueryService.markAsRead(5L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(domainClient, never()).markNotificationRead(any());
    }

    @Test
    void markAsRead_belongsToAnotherUser_throwsAccessDenied() {
        NotificationDto notification = NotificationDto.builder().id(1L).userId(5L).read(false).build();
        when(domainClient.getNotificationById(1L)).thenReturn(notification);

        assertThatThrownBy(() -> notificationQueryService.markAsRead(999L, 1L))
                .isInstanceOf(AccessDeniedException.class);

        verify(domainClient, never()).markNotificationRead(any());
    }
}
