package com.libraryapp.library.service;

import com.libraryapp.library.dto.ReservationDto;

import java.util.List;

public interface ReservationService {

    ReservationDto reserveBook(Long userId, Long bookId);

    void cancelReservation(Long userId, Long reservationId);

    List<ReservationDto> getWishlist(Long userId);

    // MANAGER
    List<ReservationDto> getReservationsForBook(Long bookId);

    // MANAGER
    void cancelReservationForBook(Long bookId, Long reservationId);

    /**
     * Called by LendingService right after a book is returned: pops the head of the book's
     * waitlist (if any), marks it NOTIFIED, and fires a notification to that user.
     */
    void processWaitlistOnReturn(Long bookId);
}
