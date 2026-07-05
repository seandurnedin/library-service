package com.libraryapp.library.controller;

import com.libraryapp.library.dto.NotificationDto;
import com.libraryapp.library.service.NotificationBroadcastService;
import com.libraryapp.library.service.NotificationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Tag(name = "Notifications", description = "Live notifications (e.g. a waitlisted book becoming available)")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationBroadcastService notificationBroadcastService;
    private final NotificationQueryService notificationQueryService;

    @Operation(summary = "Open a live SSE stream of my notifications")
    @GetMapping(value = "/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long userId, HttpServletRequest request) {
        Long authenticatedUserId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        boolean isSelf = userId.equals(authenticatedUserId);
        boolean isStaff = "ADMIN".equals(role) || "MANAGER".equals(role);
        if (!isSelf && !isStaff) {
            throw new AccessDeniedException("Cannot subscribe to another user's notification stream");
        }
        return notificationBroadcastService.subscribe(userId);
    }

    @Operation(summary = "My unread notifications - catches up on whatever fired while I wasn't connected to the live stream")
    @GetMapping("/mine")
    public List<NotificationDto> mine(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return notificationQueryService.getMyUnreadNotifications(userId);
    }

    @Operation(summary = "Mark one of my notifications as read")
    @PostMapping("/{id}/read")
    public NotificationDto markRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return notificationQueryService.markAsRead(userId, id);
    }
}
