package com.libraryapp.library.service.impl;

import com.libraryapp.library.client.DomainClient;
import com.libraryapp.library.dto.BookDto;
import com.libraryapp.library.dto.NotificationDto;
import com.libraryapp.library.dto.ReservationDto;
import com.libraryapp.library.enums.EBookStatus;
import com.libraryapp.library.enums.EReservationStatus;
import com.libraryapp.library.exception.BusinessRuleException;
import com.libraryapp.library.exception.ResourceNotFoundException;
import com.libraryapp.library.service.NotificationSenderService;
import com.libraryapp.library.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final DomainClient domainClient;
    private final NotificationSenderService notificationSenderService;

    @Override
    public ReservationDto reserveBook(Long userId, Long bookId) {
        BookDto book = domainClient.getBookById(bookId);
        if (book == null) {
            throw new ResourceNotFoundException("Book not found: " + bookId);
        }

        int available = book.getAvailableCopies() == null ? 0 : book.getAvailableCopies();
        return available > 0 ? holdAvailableCopy(userId, book) : joinWaitlist(userId, book);
    }

    /**
     * A copy is available right now, so it's taken out of general availability and held for this
     * user immediately - no need to wait in line. Reservation goes straight to NOTIFIED
     */
    private ReservationDto holdAvailableCopy(Long userId, BookDto book) {
        int remaining = book.getAvailableCopies() - 1;
        BookDto bookPatch = BookDto.builder()
                .availableCopies(remaining)
                .status(remaining == 0 ? EBookStatus.RESERVED : EBookStatus.IN_STORE)
                .build();
        domainClient.updateBook(book.getId(), bookPatch);

        ReservationDto newReservation = ReservationDto.builder()
                .status(EReservationStatus.NOTIFIED)
                .build();
        ReservationDto created = domainClient.createReservation(userId, book.getId(), newReservation);

        NotificationDto notification = NotificationDto.builder()
                .userId(userId)
                .type("BOOK_AVAILABLE")
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .message("\"" + book.getTitle() + "\" is on hold for you - it's ready for pickup.")
                .build();
        notificationSenderService.sendBookAvailableNotification(notification);

        return created;
    }

    private ReservationDto joinWaitlist(Long userId, BookDto book) {
        List<ReservationDto> existingQueue = domainClient.getReservationQueueForBook(book.getId());
        LinkedList<ReservationDto> queue = new LinkedList<>(existingQueue);
        int nextPosition = queue.size() + 1;

        ReservationDto newReservation = ReservationDto.builder()
                .status(EReservationStatus.RESERVED)
                .queuePosition(nextPosition)
                .build();

        return domainClient.createReservation(userId, book.getId(), newReservation);
    }

    @Override
    public void cancelReservation(Long userId, Long reservationId) {
        ReservationDto reservation = domainClient.getReservationsByUser(userId).stream()
                .filter(r -> r.getId().equals(reservationId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation " + reservationId + " not found for this user"));
        cancel(reservation);
    }

    @Override
    public List<ReservationDto> getReservationsForBook(Long bookId) {
        return domainClient.getActiveReservationsForBook(bookId);
    }

    @Override
    public void cancelReservationForBook(Long bookId, Long reservationId) {
        ReservationDto reservation = domainClient.getActiveReservationsForBook(bookId).stream()
                .filter(r -> r.getId().equals(reservationId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation " + reservationId + " not found for book " + bookId));
        cancel(reservation);
    }

    private void cancel(ReservationDto reservation) {
        if (reservation.getStatus() == EReservationStatus.CANCELLED
                || reservation.getStatus() == EReservationStatus.FULFILLED
                || reservation.getStatus() == EReservationStatus.EXPIRED) {
            throw new BusinessRuleException(
                    "This reservation is already " + reservation.getStatus().getValue());
        }

        boolean wasHoldingCopy = reservation.getStatus() == EReservationStatus.NOTIFIED;

        domainClient.updateReservation(reservation.getId(),
                ReservationDto.builder().status(EReservationStatus.CANCELLED).build());

        if (wasHoldingCopy) {
            releaseHeldCopy(reservation.getBookId());
            processWaitlistOnReturn(reservation.getBookId());
        }
    }

    private void releaseHeldCopy(Long bookId) {
        BookDto book = domainClient.getBookById(bookId);
        int updatedCopies = (book.getAvailableCopies() == null ? 0 : book.getAvailableCopies()) + 1;
        BookDto patch = BookDto.builder()
                .availableCopies(updatedCopies)
                .status(EBookStatus.IN_STORE)
                .build();
        domainClient.updateBook(bookId, patch);
    }

    @Override
    public List<ReservationDto> getWishlist(Long userId) {
        return domainClient.getReservationsByUser(userId).stream()
                .filter(r -> r.getStatus() == EReservationStatus.RESERVED || r.getStatus() == EReservationStatus.NOTIFIED)
                .toList();
    }

    @Override
    public void processWaitlistOnReturn(Long bookId) {
        List<ReservationDto> list = domainClient.getReservationQueueForBook(bookId);
        LinkedList<ReservationDto> queue = new LinkedList<>(list);
        ReservationDto next = queue.poll(); // FIFO: earliest reservationDate first

        if (next == null) {
            log.debug("No one waitlisted for book {}", bookId);
            return;
        }

        notifyNextInQueue(bookId, next);
    }

    private void notifyNextInQueue(Long bookId, ReservationDto next) {
        BookDto book = domainClient.getBookById(bookId);

        int heldCopies = Math.max(0, (book.getAvailableCopies() == null ? 1 : book.getAvailableCopies()) - 1);
        BookDto bookPatch = BookDto.builder()
                .availableCopies(heldCopies)
                .status(EBookStatus.RESERVED)
                .build();

        ReservationDto reservationPatch = ReservationDto.builder()
                .status(EReservationStatus.NOTIFIED)
                .build();

        NotificationDto notification = NotificationDto.builder()
                .userId(next.getUserId())
                .type("BOOK_AVAILABLE")
                .bookId(bookId)
                .bookTitle(book.getTitle())
                .message("\"" + book.getTitle() + "\" is ready for you to collect - it's on hold for 48 hours.")
                .build();

        domainClient.updateBook(bookId, bookPatch);
        domainClient.updateReservation(next.getId(), reservationPatch);
        notificationSenderService.sendBookAvailableNotification(notification);
    }
}
