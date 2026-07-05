package com.libraryapp.library.service.impl;

import com.libraryapp.library.dto.NotificationDto;
import com.libraryapp.library.service.NotificationBroadcastService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class NotificationBroadcastServiceImpl implements NotificationBroadcastService {

    private static final long NO_TIMEOUT = 0L;

    private final ConcurrentMap<Long, CopyOnWriteArrayList<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(NO_TIMEOUT);
        CopyOnWriteArrayList<SseEmitter> emitters =
                emittersByUser.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter, "completed"));
        emitter.onTimeout(() -> removeEmitter(userId, emitter, "timed out"));
        emitter.onError(ex -> removeEmitter(userId, emitter, "errored: " + ex.getMessage()));

        return emitter;
    }

    @Override
    public void publish(NotificationDto notification) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByUser.get(notification.getUserId());
        if (emitters == null || emitters.isEmpty()) {
            log.debug("No active SSE subscriber for user {}; notification dropped from live stream", notification.getUserId());
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(notification));
            } catch (Exception ex) {
                log.warn("Failed to emit notification to user {}: {}", notification.getUserId(), ex.getMessage());
                removeEmitter(notification.getUserId(), emitter, "send failed");
            }
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter, String reason) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
        log.debug("SSE subscriber for user {} disconnected ({})", userId, reason);
    }
}
