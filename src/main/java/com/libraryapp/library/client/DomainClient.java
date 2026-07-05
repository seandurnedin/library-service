package com.libraryapp.library.client;

import com.libraryapp.library.dto.*;

import java.util.List;

public interface DomainClient {

    // Users
    UserDto findUserByUsername(String username);

    UserDto findUserByEmail(String email);

    UserDto createUser(UserDto user);

    UserDto getUserById(Long userId);

    UserDto updateUser(Long userId, UserDto patch);

    List<UserDto> findUsersByRole(String role);

    // User groups
    UserGroupDto getUserGroupById(Long id);

    UserGroupDto findUserGroupByName(String name);

    UserGroupDto createUserGroup(UserGroupDto dto);

    // Books
    HalPage<BookDto> getBooksPage(int page, int size, String sortField, String sortDir);

    HalPage<BookDto> searchBooksPage(String title, int page, int size, String sortField, String sortDir);

    BookDto getBookById(Long id);

    BookDto findBookByIsbn(String isbn);

    BookDto createBook(BookDto book);

    BookDto updateBook(Long id, BookDto patch);

    void deleteBook(Long id);

    // BorrowingRecord
    BorrowingRecordDto createBorrowingRecord(Long userId, Long bookId, BorrowingRecordDto dto);

    BorrowingRecordDto getBorrowingRecordById(Long id);

    BorrowingRecordDto updateBorrowingRecord(Long id, BorrowingRecordDto patch);

    List<BorrowingRecordDto> getBorrowingRecordsByUser(Long userId);

    List<BorrowingRecordDto> getBorrowingRecordsByUserAndStatus(Long userId, String status);

    // Reservations
    ReservationDto createReservation(Long userId, Long bookId, ReservationDto dto);

    ReservationDto updateReservation(Long id, ReservationDto patch);

    List<ReservationDto> getReservationsByUser(Long userId);

    List<ReservationDto> getReservationQueueForBook(Long bookId);

    /**
     * RESERVED (queued) and NOTIFIED (holding a copy) reservations for a book
     */
    List<ReservationDto> getActiveReservationsForBook(Long bookId);

    // Payments
    PaymentDto createPayment(Long userId, Long borrowingRecordId, PaymentDto dto);

    PaymentDto getPaymentById(Long id);

    PaymentDto updatePayment(Long id, PaymentDto patch);

    List<PaymentDto> getPaymentsByUser(Long userId);

    // Notifications
    NotificationDto createNotification(Long userId, NotificationDto dto);

    NotificationDto getNotificationById(Long id);

    NotificationDto markNotificationRead(Long id);

    List<NotificationDto> getUnreadNotificationsByUser(Long userId);

    UserProfileDto getUserProfile(Long userId);

    BookAvailabilityDto getBookAvailability(Long bookId);

    List<UserBorrowingHistoryDto> getUserBorrowingHistory(Long userId);

    List<OverdueRecordDto> getOverdueRecords();
}
