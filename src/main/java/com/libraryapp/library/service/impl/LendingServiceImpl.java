package com.libraryapp.library.service.impl;

import com.libraryapp.library.client.DomainClient;
import com.libraryapp.library.dto.*;
import com.libraryapp.library.enums.*;
import com.libraryapp.library.exception.BusinessRuleException;
import com.libraryapp.library.exception.ResourceNotFoundException;
import com.libraryapp.library.service.LendingService;
import com.libraryapp.library.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LendingServiceImpl implements LendingService {

    private static final int DEFAULT_LOAN_DAYS = 14;
    private static final BigDecimal DEFAULT_DAILY_LATE_FEE = new BigDecimal("0.50");

    private final DomainClient domainClient;
    private final ReservationService reservationService;

    @Override
    public BorrowingRecordDto loanBook(String username, String isbn) {
        UserDto user = domainClient.findUserByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("User not found: " + username);
        }
        BookDto book = domainClient.findBookByIsbn(isbn);
        if (book == null) {
            throw new ResourceNotFoundException("Book not found for ISBN: " + isbn);
        }
        UserProfileDto profile = domainClient.getUserProfile(user.getId());
        if (profile == null) {
            throw new ResourceNotFoundException("User not found: " + username);
        }

        ReservationDto heldReservation = domainClient.getReservationsByUser(user.getId()).stream()
                .filter(r -> book.getId().equals(r.getBookId()) && r.getStatus() == EReservationStatus.NOTIFIED)
                .findFirst()
                .orElse(null);

        return heldReservation != null
                ? fulfillHeldReservation(profile, book, heldReservation)
                : validateAndCreateLoan(profile, book);
    }

    /**
     * A copy was already pulled out of general availability when this reservation was placed on
     * unlike validateAndCreateLoan this must NOT decrement availableCopies again
     */
    private BorrowingRecordDto fulfillHeldReservation(UserProfileDto profile, BookDto book, ReservationDto reservation) {
        int maxAllowed = profile.getMaxBooksAllowed() != null ? profile.getMaxBooksAllowed() : 5;
        int activeLoanCount = profile.getActiveLoanCount() != null ? profile.getActiveLoanCount() : 0;
        if (activeLoanCount >= maxAllowed) {
            throw new BusinessRuleException(
                    "Borrow limit reached (" + maxAllowed + " books) - return a book before borrowing another");
        }

        int loanDays = profile.getLoanDurationDays() != null ? profile.getLoanDurationDays() : DEFAULT_LOAN_DAYS;
        LocalDate today = LocalDate.now();

        BorrowingRecordDto request = BorrowingRecordDto.builder()
                .borrowDate(today)
                .dueDate(today.plusDays(loanDays))
                .status(EBorrowingStatus.ON_LOAN)
                .lateFee(BigDecimal.ZERO.setScale(2))
                .build();

        BorrowingRecordDto created = domainClient.createBorrowingRecord(profile.getUserId(), book.getId(), request);
        domainClient.updateReservation(reservation.getId(),
                ReservationDto.builder().status(EReservationStatus.FULFILLED).build());
        return created;
    }

    private BorrowingRecordDto validateAndCreateLoan(UserProfileDto profile, BookDto book) {
        if (book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            throw new BusinessRuleException(
                    "\"" + book.getTitle() + "\" has no available copies right now - join the waitlist instead");
        }

        int maxAllowed = profile.getMaxBooksAllowed() != null ? profile.getMaxBooksAllowed() : 5;
        int activeLoanCount = profile.getActiveLoanCount() != null ? profile.getActiveLoanCount() : 0;
        if (activeLoanCount >= maxAllowed) {
            throw new BusinessRuleException(
                    "Borrow limit reached (" + maxAllowed + " books) - return a book before borrowing another");
        }

        int loanDays = profile.getLoanDurationDays() != null ? profile.getLoanDurationDays() : DEFAULT_LOAN_DAYS;
        LocalDate today = LocalDate.now();

        BorrowingRecordDto request = BorrowingRecordDto.builder()
                .borrowDate(today)
                .dueDate(today.plusDays(loanDays))
                .status(EBorrowingStatus.ON_LOAN)
                .lateFee(BigDecimal.ZERO.setScale(2))
                .build();

        int remaining = book.getAvailableCopies() - 1;
        BookDto bookPatch = BookDto.builder()
                .availableCopies(remaining)
                .status(remaining == 0 ? EBookStatus.ON_LOAN : EBookStatus.IN_STORE)
                .build();

        BorrowingRecordDto created = domainClient.createBorrowingRecord(profile.getUserId(), book.getId(), request);
        domainClient.updateBook(book.getId(), bookPatch);
        return created;
    }

    @Override
    public BorrowingRecordDto returnBook(String username, String isbn) {
        UserDto user = domainClient.findUserByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("User not found: " + username);
        }
        BookDto book = domainClient.findBookByIsbn(isbn);
        if (book == null) {
            throw new ResourceNotFoundException("Book not found for ISBN: " + isbn);
        }

        // on loan and overdue
        List<BorrowingRecordDto> active = new ArrayList<>(
                domainClient.getBorrowingRecordsByUserAndStatus(user.getId(), EBorrowingStatus.ON_LOAN.name()));
        active.addAll(domainClient.getBorrowingRecordsByUserAndStatus(user.getId(), EBorrowingStatus.OVERDUE.name()));

        BorrowingRecordDto record = active.stream()
                .filter(r -> book.getId().equals(r.getBookId()))
                .min(Comparator.comparing(BorrowingRecordDto::getBorrowDate))
                .orElseThrow(() -> new BusinessRuleException(
                        "\"" + username + "\" has no active loan for ISBN " + isbn));

        LocalDate today = LocalDate.now();
        long daysLate = Math.max(0, ChronoUnit.DAYS.between(record.getDueDate(), today));
        BigDecimal lateFee = DEFAULT_DAILY_LATE_FEE.multiply(BigDecimal.valueOf(daysLate)).setScale(2, RoundingMode.HALF_UP);

        BorrowingRecordDto patch = BorrowingRecordDto.builder()
                .returnDate(today)
                .status(EBorrowingStatus.RETURNED)
                .lateFee(lateFee)
                .build();

        BorrowingRecordDto updated = domainClient.updateBorrowingRecord(record.getId(), patch);

        if (lateFee.compareTo(BigDecimal.ZERO) > 0) {
            PaymentDto lateFeeCharge = PaymentDto.builder()
                    .amount(lateFee)
                    .type(EPaymentType.LATE_FEE)
                    .status(EPaymentStatus.PENDING)
                    .build();
            domainClient.createPayment(user.getId(), record.getId(), lateFeeCharge);
        }

        releaseBookCopy(record.getBookId());
        reservationService.processWaitlistOnReturn(record.getBookId());
        return updated;
    }

    private void releaseBookCopy(Long bookId) {
        BookDto book = domainClient.getBookById(bookId);
        int updatedCopies = (book.getAvailableCopies() == null ? 0 : book.getAvailableCopies()) + 1;
        BookDto patch = BookDto.builder()
                .availableCopies(updatedCopies)
                .status(EBookStatus.IN_STORE)
                .build();
        domainClient.updateBook(bookId, patch);
    }

    @Override
    public List<BorrowingRecordDto> getLoanedBooks(Long userId) {
        List<BorrowingRecordDto> loans = new ArrayList<>(
                domainClient.getBorrowingRecordsByUserAndStatus(userId, EBorrowingStatus.ON_LOAN.name()));
        loans.addAll(domainClient.getBorrowingRecordsByUserAndStatus(userId, EBorrowingStatus.OVERDUE.name()));
        return loans;
    }

    @Override
    public List<BorrowingRecordDto> getLoansForBook(Long bookId) {
        return domainClient.getActiveBorrowingRecordsForBook(bookId);
    }
}
