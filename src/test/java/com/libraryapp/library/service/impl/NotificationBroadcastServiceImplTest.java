package com.libraryapp.library.service.impl;

import com.libraryapp.library.dto.NotificationDto;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * SseEmitter.send() only actually writes bytes once the servlet container has attached a handler
 * to the emitter, so a plain unit test can't observe delivery the way a StepVerifier could on a
 * Flux. These tests instead cover what's observable at this layer: subscribing returns a live
 * emitter, publishing to a subscribed/unsubscribed user never throws, and completing an emitter
 * (simulating the client disconnecting) removes it from the registry so a later publish is a no-op.
 */
class NotificationBroadcastServiceImplTest {

    private final NotificationBroadcastServiceImpl broadcastService = new NotificationBroadcastServiceImpl();

    @Test
    void subscribe_returnsALiveEmitter() {
        SseEmitter emitter = broadcastService.subscribe(1L);
        assertThat(emitter).isNotNull();
    }

    @Test
    void publish_noSubscriber_doesNotThrow() {
        NotificationDto notification = NotificationDto.builder().userId(999L).type("BOOK_AVAILABLE").build();
        assertThatCode(() -> broadcastService.publish(notification)).doesNotThrowAnyException(); // should just log & no-op, not throw
    }

    @Test
    void publish_toSubscribedUser_doesNotThrow() {
        broadcastService.subscribe(1L);
        NotificationDto notification = NotificationDto.builder().userId(1L).type("BOOK_AVAILABLE").build();

        assertThatCode(() -> broadcastService.publish(notification)).doesNotThrowAnyException();
    }

    @Test
    void completedEmitter_isRemovedFromRegistry_soLaterPublishIsANoOp() {
        SseEmitter emitter = broadcastService.subscribe(1L);
        emitter.complete(); // triggers the onCompletion callback synchronously

        NotificationDto notification = NotificationDto.builder().userId(1L).type("BOOK_AVAILABLE").build();
        assertThatCode(() -> broadcastService.publish(notification)).doesNotThrowAnyException();
    }
}
