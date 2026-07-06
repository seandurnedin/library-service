package com.libraryapp.library.service.impl;

import com.libraryapp.library.client.DomainClient;
import com.libraryapp.library.dto.BookDto;
import com.libraryapp.library.dto.ReservationDto;
import com.libraryapp.library.enums.EBookStatus;
import com.libraryapp.library.enums.EReservationStatus;
import com.libraryapp.library.exception.BusinessRuleException;
import com.libraryapp.library.exception.ResourceNotFoundException;
import com.libraryapp.library.service.NotificationSenderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock
    private DomainClient domainClient;
    @Mock
    private NotificationSenderService notificationSenderService;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    @Test
    void reserveBook_bookNotFound_throws() {
        when(domainClient.getBookById(99L)).thenReturn(null);

        assertThatThrownBy(() -> reservationService.reserveBook(3L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void processWaitlistOnReturn_emptyQueue_doesNothing() {
        when(domainClient.getReservationQueueForBook(10L)).thenReturn(List.of());

        reservationService.processWaitlistOnReturn(10L);

        verifyNoInteractions(notificationSenderService);
    }

    @Test
    void processWaitlistOnReturn_popsHeadOfQueue_fifoOrder() {
        // FIFO: reservation "1" (earlier) must be notified before reservation "2".
        ReservationDto first = ReservationDto.builder().id(1L).userId(2L).bookId(10L).status(EReservationStatus.RESERVED).build();
        ReservationDto second = ReservationDto.builder().id(2L).userId(3L).bookId(10L).status(EReservationStatus.RESERVED).build();
        when(domainClient.getReservationQueueForBook(10L)).thenReturn(List.of(first, second));

        BookDto book = BookDto.builder().id(10L).title("Effective Java").availableCopies(1).build();
        when(domainClient.getBookById(10L)).thenReturn(book);
        when(domainClient.updateBook(eq(10L), any(BookDto.class))).thenReturn(book);
        when(domainClient.updateReservation(eq(1L), any(ReservationDto.class))).thenReturn(first);

        reservationService.processWaitlistOnReturn(10L);

        verify(domainClient).updateReservation(eq(1L), argThat(p -> p.getStatus() == EReservationStatus.NOTIFIED));
        verify(domainClient, never()).updateReservation(eq(2L), any());
        verify(domainClient).updateBook(eq(10L), argThat(p -> p.getStatus() == EBookStatus.RESERVED && p.getAvailableCopies() == 0));
        verify(notificationSenderService).sendBookAvailableNotification(argThat(n -> n.getUserId().equals(2L)));
    }

    @Test
    void cancelReservation_ownedByUser_cancelsIt() {
        ReservationDto reservation = ReservationDto.builder().id(5L).userId(1L).status(EReservationStatus.RESERVED).build();
        when(domainClient.getReservationsByUser(1L)).thenReturn(List.of(reservation));
        when(domainClient.updateReservation(eq(5L), any(ReservationDto.class))).thenReturn(reservation);

        reservationService.cancelReservation(1L, 5L);

        verify(domainClient).updateReservation(eq(5L), argThat(p -> p.getStatus() == EReservationStatus.CANCELLED));
    }

    @Test
    void cancelReservation_notOwnedByUser_throwsNotFound() {
        when(domainClient.getReservationsByUser(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> reservationService.cancelReservation(1L, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelReservation_alreadyFulfilled_throwsBusinessRule() {
        ReservationDto fulfilled = ReservationDto.builder().id(5L).userId(1L).status(EReservationStatus.FULFILLED).build();
        when(domainClient.getReservationsByUser(1L)).thenReturn(List.of(fulfilled));

        assertThatThrownBy(() -> reservationService.cancelReservation(1L, 5L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cancelReservation_heldCopy_releasesItAndOffersToNextInQueue() {
        ReservationDto held = ReservationDto.builder().id(5L).userId(1L).bookId(10L).status(EReservationStatus.NOTIFIED).build();
        when(domainClient.getReservationsByUser(1L)).thenReturn(List.of(held));
        when(domainClient.updateReservation(eq(5L), any(ReservationDto.class))).thenReturn(held);

        BookDto book = BookDto.builder().id(10L).availableCopies(0).build();
        when(domainClient.getBookById(10L)).thenReturn(book);
        when(domainClient.updateBook(eq(10L), any(BookDto.class))).thenReturn(book);
        when(domainClient.getReservationQueueForBook(10L)).thenReturn(List.of());

        reservationService.cancelReservation(1L, 5L);

        verify(domainClient).updateReservation(eq(5L), argThat(p -> p.getStatus() == EReservationStatus.CANCELLED));
        verify(domainClient).updateBook(eq(10L), argThat(p -> p.getAvailableCopies() == 1 && p.getStatus() == EBookStatus.IN_STORE));
    }

    @Test
    void getWishlist_filtersToActiveReservationsOnly() {
        ReservationDto active = ReservationDto.builder().id(1L).status(EReservationStatus.RESERVED).build();
        ReservationDto cancelled = ReservationDto.builder().id(2L).status(EReservationStatus.CANCELLED).build();
        when(domainClient.getReservationsByUser(1L)).thenReturn(List.of(active, cancelled));

        List<ReservationDto> wishlist = reservationService.getWishlist(1L);

        assertThat(wishlist).containsExactly(active);
    }
}
