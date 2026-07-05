package com.libraryapp.library.service.impl;

import com.libraryapp.library.client.DomainClient;
import com.libraryapp.library.dto.BookDto;
import com.libraryapp.library.dto.BorrowingRecordDto;
import com.libraryapp.library.dto.ReservationDto;
import com.libraryapp.library.dto.UserDto;
import com.libraryapp.library.dto.UserProfileDto;
import com.libraryapp.library.enums.EBorrowingStatus;
import com.libraryapp.library.enums.EPaymentStatus;
import com.libraryapp.library.enums.EPaymentType;
import com.libraryapp.library.enums.EReservationStatus;
import com.libraryapp.library.exception.BusinessRuleException;
import com.libraryapp.library.exception.ResourceNotFoundException;
import com.libraryapp.library.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LendingServiceImplTest {

    private static final String USERNAME = "jdoe";
    private static final String ISBN = "9780134685991";

    @Mock
    private DomainClient domainClient;
    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private LendingServiceImpl lendingService;

    private UserDto user;
    private UserProfileDto profile;
    private BookDto book;

    @BeforeEach
    void setUp() {
        user = UserDto.builder().id(1L).username(USERNAME).build();
        profile = UserProfileDto.builder()
                .userId(1L).maxBooksAllowed(5).loanDurationDays(14).activeLoanCount(1)
                .build();
        book = BookDto.builder().id(10L).isbn(ISBN).title("Effective Java").availableCopies(2).totalCopies(3).build();
    }

    @Test
    void loanBook_happyPath_createsRecordAndDecrementsCopies() {
        when(domainClient.findUserByUsername(USERNAME)).thenReturn(user);
        when(domainClient.findBookByIsbn(ISBN)).thenReturn(book);
        when(domainClient.getUserProfile(1L)).thenReturn(profile);

        BorrowingRecordDto record = BorrowingRecordDto.builder()
                .id(100L).userId(1L).bookId(10L).status(EBorrowingStatus.ON_LOAN)
                .borrowDate(LocalDate.now()).dueDate(LocalDate.now().plusDays(14))
                .build();
        when(domainClient.createBorrowingRecord(eq(1L), eq(10L), any(BorrowingRecordDto.class)))
                .thenReturn(record);
        when(domainClient.updateBook(eq(10L), any(BookDto.class))).thenReturn(book);

        BorrowingRecordDto r = lendingService.loanBook(USERNAME, ISBN);

        assertThat(r.getId()).isEqualTo(100L);
        verify(domainClient).updateBook(eq(10L), argThat(patch -> patch.getAvailableCopies() == 1));
    }

    @Test
    void loanBook_noAvailableCopies_throwsBusinessRule() {
        BookDto outOfStock = BookDto.builder().id(10L).isbn(ISBN).title("Rare Book").availableCopies(0).build();
        when(domainClient.findUserByUsername(USERNAME)).thenReturn(user);
        when(domainClient.findBookByIsbn(ISBN)).thenReturn(outOfStock);
        when(domainClient.getUserProfile(1L)).thenReturn(profile);

        assertThatThrownBy(() -> lendingService.loanBook(USERNAME, ISBN))
                .isInstanceOf(BusinessRuleException.class);

        verify(domainClient, never()).createBorrowingRecord(any(), any(), any());
    }

    @Test
    void loanBook_atBorrowLimit_throwsBusinessRule() {
        UserProfileDto atLimit = UserProfileDto.builder()
                .userId(1L).maxBooksAllowed(2).activeLoanCount(2).loanDurationDays(14).build();
        when(domainClient.findUserByUsername(USERNAME)).thenReturn(user);
        when(domainClient.findBookByIsbn(ISBN)).thenReturn(book);
        when(domainClient.getUserProfile(1L)).thenReturn(atLimit);

        assertThatThrownBy(() -> lendingService.loanBook(USERNAME, ISBN))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void loanBook_userNotFound_throws() {
        when(domainClient.findUserByUsername(USERNAME)).thenReturn(null);

        assertThatThrownBy(() -> lendingService.loanBook(USERNAME, ISBN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void loanBook_bookNotFound_throws() {
        when(domainClient.findUserByUsername(USERNAME)).thenReturn(user);
        when(domainClient.findBookByIsbn(ISBN)).thenReturn(null);

        assertThatThrownBy(() -> lendingService.loanBook(USERNAME, ISBN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void loanBook_hasHeldReservation_fulfillsItWithoutDecrementingCopiesAgain() {
        when(domainClient.findUserByUsername(USERNAME)).thenReturn(user);
        when(domainClient.findBookByIsbn(ISBN)).thenReturn(book);
        when(domainClient.getUserProfile(1L)).thenReturn(profile);

        ReservationDto held = ReservationDto.builder().id(50L).userId(1L).bookId(10L).status(EReservationStatus.NOTIFIED).build();
        when(domainClient.getReservationsByUser(1L)).thenReturn(List.of(held));

        BorrowingRecordDto record = BorrowingRecordDto.builder()
                .id(100L).userId(1L).bookId(10L).status(EBorrowingStatus.ON_LOAN)
                .borrowDate(LocalDate.now()).dueDate(LocalDate.now().plusDays(14))
                .build();
        when(domainClient.createBorrowingRecord(eq(1L), eq(10L), any(BorrowingRecordDto.class)))
                .thenReturn(record);
        when(domainClient.updateReservation(eq(50L), any(ReservationDto.class)))
                .thenReturn(ReservationDto.builder().id(50L).status(EReservationStatus.FULFILLED).build());

        BorrowingRecordDto r = lendingService.loanBook(USERNAME, ISBN);

        assertThat(r.getId()).isEqualTo(100L);
        verify(domainClient).updateReservation(eq(50L), argThat(p -> p.getStatus() == EReservationStatus.FULFILLED));
        verify(domainClient, never()).updateBook(any(), any());
    }

    @Test
    void returnBook_onTime_noLateFee() {
        BorrowingRecordDto onLoan = BorrowingRecordDto.builder()
                .id(100L).bookId(10L).status(EBorrowingStatus.ON_LOAN)
                .borrowDate(LocalDate.now().minusDays(5)).dueDate(LocalDate.now().plusDays(5))
                .build();
        when(domainClient.findUserByUsername(USERNAME)).thenReturn(user);
        when(domainClient.findBookByIsbn(ISBN)).thenReturn(book);
        when(domainClient.getBorrowingRecordsByUserAndStatus(1L, EBorrowingStatus.ON_LOAN.name())).thenReturn(List.of(onLoan));
        when(domainClient.getBorrowingRecordsByUserAndStatus(1L, EBorrowingStatus.OVERDUE.name())).thenReturn(Collections.emptyList());

        BorrowingRecordDto returned = BorrowingRecordDto.builder()
                .id(100L).bookId(10L).status(EBorrowingStatus.RETURNED).lateFee(BigDecimal.ZERO).build();
        when(domainClient.updateBorrowingRecord(eq(100L), argThat(p -> p.getLateFee().compareTo(BigDecimal.ZERO) == 0)))
                .thenReturn(returned);
        when(domainClient.getBookById(10L)).thenReturn(book);
        when(domainClient.updateBook(eq(10L), any(BookDto.class))).thenReturn(book);

        BorrowingRecordDto r = lendingService.returnBook(USERNAME, ISBN);

        assertThat(r.getStatus()).isEqualTo(EBorrowingStatus.RETURNED);
        verify(reservationService).processWaitlistOnReturn(10L);
        verify(domainClient, never()).createPayment(any(), any(), any());
    }

    @Test
    void returnBook_overdue_calculatesLateFee() {
        BorrowingRecordDto overdue = BorrowingRecordDto.builder()
                .id(100L).bookId(10L).status(EBorrowingStatus.ON_LOAN)
                .borrowDate(LocalDate.now().minusDays(20)).dueDate(LocalDate.now().minusDays(3))
                .build();
        when(domainClient.findUserByUsername(USERNAME)).thenReturn(user);
        when(domainClient.findBookByIsbn(ISBN)).thenReturn(book);
        when(domainClient.getBorrowingRecordsByUserAndStatus(1L, EBorrowingStatus.ON_LOAN.name())).thenReturn(List.of(overdue));
        when(domainClient.getBorrowingRecordsByUserAndStatus(1L, EBorrowingStatus.OVERDUE.name())).thenReturn(Collections.emptyList());
        when(domainClient.updateBorrowingRecord(eq(100L), argThat(p -> p.getLateFee().compareTo(new BigDecimal("1.50")) == 0)))
                .thenReturn(BorrowingRecordDto.builder().id(100L).lateFee(new BigDecimal("1.50")).status(EBorrowingStatus.RETURNED).build());
        when(domainClient.getBookById(10L)).thenReturn(book);
        when(domainClient.updateBook(eq(10L), any(BookDto.class))).thenReturn(book);

        BorrowingRecordDto r = lendingService.returnBook(USERNAME, ISBN);

        assertThat(r.getLateFee()).isEqualByComparingTo(new BigDecimal("1.50")); // 3 days * 0.50
        verify(domainClient).createPayment(eq(1L), eq(100L), argThat(p ->
                p.getType() == EPaymentType.LATE_FEE
                        && p.getStatus() == EPaymentStatus.PENDING
                        && p.getAmount().compareTo(new BigDecimal("1.50")) == 0));
    }

    @Test
    void returnBook_noActiveLoanForIsbn_throwsBusinessRule() {
        when(domainClient.findUserByUsername(USERNAME)).thenReturn(user);
        when(domainClient.findBookByIsbn(ISBN)).thenReturn(book);
        when(domainClient.getBorrowingRecordsByUserAndStatus(1L, EBorrowingStatus.ON_LOAN.name())).thenReturn(Collections.emptyList());
        when(domainClient.getBorrowingRecordsByUserAndStatus(1L, EBorrowingStatus.OVERDUE.name())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> lendingService.returnBook(USERNAME, ISBN))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void returnBook_userNotFound_throws() {
        when(domainClient.findUserByUsername(USERNAME)).thenReturn(null);

        assertThatThrownBy(() -> lendingService.returnBook(USERNAME, ISBN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void returnBook_bookNotFound_throws() {
        when(domainClient.findUserByUsername(USERNAME)).thenReturn(user);
        when(domainClient.findBookByIsbn(ISBN)).thenReturn(null);

        assertThatThrownBy(() -> lendingService.returnBook(USERNAME, ISBN))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
