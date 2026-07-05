package com.libraryapp.library.client.impl;

import com.libraryapp.library.client.DomainClient;
import com.libraryapp.library.config.DomainServiceProperties;
import com.libraryapp.library.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.libraryapp.library.config.DomainServiceProperties.Endpoints;
import static com.libraryapp.library.config.DomainServiceProperties.Resources;

@Slf4j
@Component
public class DomainClientImpl implements DomainClient {

    private final RestClient restClient;
    private final DomainServiceProperties domainServiceProperties;

    public DomainClientImpl(@Qualifier("domainServiceRestClient") RestClient restClient,
                            DomainServiceProperties domainServiceProperties) {
        this.restClient = restClient;
        this.domainServiceProperties = domainServiceProperties;
    }

    private String link(String resource, Long id) {
        return domainServiceProperties.getBaseUrl() + "/api/" + resource + "/" + id;
    }

    /**
     * Any 404 from a single-resource GET/search is treated as "not found" -> null, not an error.
     */
    private <T> T notFoundAsNull(Supplier<T> call) {
        try {
            return call.get();
        } catch (HttpClientErrorException.NotFound ex) {
            return null;
        }
    }

    @Override
    public UserDto findUserByUsername(String username) {
        return notFoundAsNull(() -> restClient.get()
                .uri(Endpoints.USERS_BY_USERNAME, username)
                .retrieve()
                .body(UserDto.class));
    }

    @Override
    public UserDto findUserByEmail(String email) {
        return notFoundAsNull(() -> restClient.get()
                .uri(Endpoints.USERS_BY_EMAIL, email)
                .retrieve()
                .body(UserDto.class));
    }

    @Override
    public UserDto createUser(UserDto user) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", user.getUsername());
        body.put("email", user.getEmail());
        body.put("passwordHash", user.getPasswordHash());
        body.put("fullName", user.getFullName());
        body.put("phone", user.getPhone());
        body.put("address", user.getAddress());
        body.put("status", user.getStatus() != null ? user.getStatus().name() : "ACTIVE");
        body.put("role", user.getRole() != null ? user.getRole().name() : "USER");
        if (user.getUserGroupId() != null) {
            body.put("userGroup", link(Resources.USER_GROUPS, user.getUserGroupId()));
        }
        return restClient.post().uri(Endpoints.USERS)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(UserDto.class);
    }

    @Override
    public UserDto getUserById(Long userId) {
        return notFoundAsNull(() -> restClient.get().uri(Endpoints.USER_BY_ID, userId)
                .retrieve()
                .body(UserDto.class));
    }

    @Override
    public UserDto updateUser(Long userId, UserDto patch) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (patch.getStatus() != null) body.put("status", patch.getStatus().name());
        if (patch.getRole() != null) body.put("role", patch.getRole().name());
        if (patch.getFullName() != null) body.put("fullName", patch.getFullName());
        if (patch.getPhone() != null) body.put("phone", patch.getPhone());
        if (patch.getAddress() != null) body.put("address", patch.getAddress());
        if (patch.getUserGroupId() != null) body.put("userGroup", link(Resources.USER_GROUPS, patch.getUserGroupId()));
        return restClient.patch().uri(Endpoints.USER_BY_ID, userId)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(UserDto.class);
    }

    @Override
    public List<UserDto> findUsersByRole(String role) {
        HalPage<UserDto> page = restClient.get().uri(Endpoints.USERS_BY_ROLE, role)
                .retrieve()
                .body(new ParameterizedTypeReference<HalPage<UserDto>>() {
                });
        return page.content();
    }

    @Override
    public UserGroupDto getUserGroupById(Long id) {
        return notFoundAsNull(() -> restClient.get().uri(Endpoints.USER_GROUP_BY_ID, id)
                .retrieve()
                .body(UserGroupDto.class));
    }

    @Override
    public UserGroupDto findUserGroupByName(String name) {
        return notFoundAsNull(() -> restClient.get().uri(Endpoints.USER_GROUPS_BY_NAME, name)
                .retrieve()
                .body(UserGroupDto.class));
    }

    @Override
    public UserGroupDto createUserGroup(UserGroupDto dto) {
        return restClient.post().uri(Endpoints.USER_GROUPS)
                .accept(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(UserGroupDto.class);
    }

    @Override
    public HalPage<BookDto> getBooksPage(int page, int size, String sortField, String sortDir) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path(Endpoints.BOOKS)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .queryParam("sort", sortField + "," + sortDir)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<HalPage<BookDto>>() {
                });
    }

    @Override
    public HalPage<BookDto> searchBooksPage(String title, int page, int size, String sortField, String sortDir) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path(Endpoints.BOOKS_SEARCH_BY_TITLE)
                        .queryParam("title", title)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .queryParam("sort", sortField + "," + sortDir)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<HalPage<BookDto>>() {
                });
    }

    @Override
    public BookDto getBookById(Long id) {
        return notFoundAsNull(() -> restClient.get().uri(Endpoints.BOOK_BY_ID, id)
                .retrieve()
                .body(BookDto.class));
    }

    @Override
    public BookDto findBookByIsbn(String isbn) {
        return notFoundAsNull(() -> restClient.get().uri(Endpoints.BOOKS_BY_ISBN, isbn)
                .retrieve()
                .body(BookDto.class));
    }

    @Override
    public BookDto createBook(BookDto book) {
        return restClient.post().uri(Endpoints.BOOKS)
                .accept(MediaType.APPLICATION_JSON)
                .body(book)
                .retrieve()
                .body(BookDto.class);
    }

    @Override
    public BookDto updateBook(Long id, BookDto patch) {
        // Built as a sparse map (only non-null fields) rather than sending the whole BookDto -
        // a JSON PATCH body that includes an explicit "title": null would blank out the title.
        Map<String, Object> body = new LinkedHashMap<>();
        if (patch.getTitle() != null) body.put("title", patch.getTitle());
        if (patch.getAuthor() != null) body.put("author", patch.getAuthor());
        if (patch.getPublisher() != null) body.put("publisher", patch.getPublisher());
        if (patch.getPublishedYear() != null) body.put("publishedYear", patch.getPublishedYear());
        if (patch.getGenre() != null) body.put("genre", patch.getGenre());
        if (patch.getTotalCopies() != null) body.put("totalCopies", patch.getTotalCopies());
        if (patch.getAvailableCopies() != null) body.put("availableCopies", patch.getAvailableCopies());
        if (patch.getStatus() != null) body.put("status", patch.getStatus().name());
        return restClient.patch().uri(Endpoints.BOOK_BY_ID, id)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(BookDto.class);
    }

    @Override
    public void deleteBook(Long id) {
        restClient.delete().uri(Endpoints.BOOK_BY_ID, id)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public BorrowingRecordDto createBorrowingRecord(Long userId, Long bookId, BorrowingRecordDto dto) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user", link(Resources.USERS, userId));
        body.put("book", link(Resources.BOOKS, bookId));
        body.put("borrowDate", dto.getBorrowDate().toString());
        body.put("dueDate", dto.getDueDate().toString());
        body.put("status", dto.getStatus().name());
        body.put("lateFee", dto.getLateFee() != null ? dto.getLateFee() : 0.0);
        return restClient.post().uri(Endpoints.BORROWING_RECORDS)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(BorrowingRecordDto.class);
    }

    @Override
    public BorrowingRecordDto getBorrowingRecordById(Long id) {
        return notFoundAsNull(() -> restClient.get().uri(Endpoints.BORROWING_RECORD_BY_ID, id)
                .retrieve()
                .body(BorrowingRecordDto.class));
    }

    @Override
    public BorrowingRecordDto updateBorrowingRecord(Long id, BorrowingRecordDto patch) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (patch.getReturnDate() != null) body.put("returnDate", patch.getReturnDate().toString());
        if (patch.getStatus() != null) body.put("status", patch.getStatus().name());
        if (patch.getLateFee() != null) body.put("lateFee", patch.getLateFee());
        return restClient.patch().uri(Endpoints.BORROWING_RECORD_BY_ID, id)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(BorrowingRecordDto.class);
    }

    @Override
    public List<BorrowingRecordDto> getBorrowingRecordsByUser(Long userId) {
        HalPage<BorrowingRecordDto> page = restClient.get()
                .uri(Endpoints.BORROWING_RECORDS_BY_USER, userId)
                .retrieve()
                .body(new ParameterizedTypeReference<HalPage<BorrowingRecordDto>>() {
                });
        return page.content();
    }

    @Override
    public List<BorrowingRecordDto> getBorrowingRecordsByUserAndStatus(Long userId, String status) {
        HalPage<BorrowingRecordDto> page = restClient.get()
                .uri(Endpoints.BORROWING_RECORDS_BY_USER_AND_STATUS, userId, status)
                .retrieve()
                .body(new ParameterizedTypeReference<HalPage<BorrowingRecordDto>>() {
                });
        return page.content();
    }

    @Override
    public ReservationDto createReservation(Long userId, Long bookId, ReservationDto dto) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user", link(Resources.USERS, userId));
        body.put("book", link(Resources.BOOKS, bookId));
        body.put("status", dto.getStatus().name());
        body.put("queuePosition", dto.getQueuePosition());
        return restClient.post().uri(Endpoints.RESERVATIONS)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(ReservationDto.class);
    }

    @Override
    public ReservationDto updateReservation(Long id, ReservationDto patch) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (patch.getStatus() != null) body.put("status", patch.getStatus().name());
        if (patch.getQueuePosition() != null) body.put("queuePosition", patch.getQueuePosition());
        return restClient.patch().uri(Endpoints.RESERVATION_BY_ID, id)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(ReservationDto.class);
    }

    @Override
    public List<ReservationDto> getReservationsByUser(Long userId) {
        HalPage<ReservationDto> page = restClient.get().uri(Endpoints.RESERVATIONS_BY_USER, userId)
                .retrieve()
                .body(new ParameterizedTypeReference<HalPage<ReservationDto>>() {
                });
        return page.content();
    }

    @Override
    public List<ReservationDto> getReservationQueueForBook(Long bookId) {
        HalPage<ReservationDto> page = restClient.get()
                .uri(Endpoints.RESERVATIONS_QUEUE_FOR_BOOK, bookId)
                .retrieve()
                .body(new ParameterizedTypeReference<HalPage<ReservationDto>>() {
                });
        return page.content();
    }

    @Override
    public List<ReservationDto> getActiveReservationsForBook(Long bookId) {
        HalPage<ReservationDto> page = restClient.get()
                .uri(Endpoints.RESERVATIONS_ACTIVE_FOR_BOOK, bookId)
                .retrieve()
                .body(new ParameterizedTypeReference<HalPage<ReservationDto>>() {
                });
        return page.content();
    }

    @Override
    public PaymentDto createPayment(Long userId, Long borrowingRecordId, PaymentDto dto) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user", link(Resources.USERS, userId));
        if (borrowingRecordId != null)
            body.put("borrowingRecord", link(Resources.BORROWING_RECORDS, borrowingRecordId));
        body.put("amount", dto.getAmount());
        body.put("type", dto.getType().name());
        body.put("status", dto.getStatus() != null ? dto.getStatus().name() : "PENDING");
        return restClient.post().uri(Endpoints.PAYMENTS)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(PaymentDto.class);
    }

    @Override
    public PaymentDto getPaymentById(Long id) {
        return notFoundAsNull(() -> restClient.get().uri(Endpoints.PAYMENT_BY_ID, id)
                .retrieve()
                .body(PaymentDto.class));
    }

    @Override
    public PaymentDto updatePayment(Long id, PaymentDto patch) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (patch.getStatus() != null) body.put("status", patch.getStatus().name());
        return restClient.patch().uri(Endpoints.PAYMENT_BY_ID, id)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(PaymentDto.class);
    }

    @Override
    public List<PaymentDto> getPaymentsByUser(Long userId) {
        HalPage<PaymentDto> page = restClient.get().uri(Endpoints.PAYMENTS_BY_USER, userId)
                .retrieve()
                .body(new ParameterizedTypeReference<HalPage<PaymentDto>>() {
                });
        return page.content();
    }

    @Override
    public NotificationDto createNotification(Long userId, NotificationDto dto) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user", link(Resources.USERS, userId));
        body.put("type", dto.getType());
        body.put("message", dto.getMessage());
        body.put("bookId", dto.getBookId());
        body.put("bookTitle", dto.getBookTitle());
        return restClient.post().uri(Endpoints.NOTIFICATIONS)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(NotificationDto.class);
    }

    @Override
    public NotificationDto getNotificationById(Long id) {
        return notFoundAsNull(() -> restClient.get().uri(Endpoints.NOTIFICATION_BY_ID, id)
                .retrieve()
                .body(NotificationDto.class));
    }

    @Override
    public NotificationDto markNotificationRead(Long id) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("read", true);
        return restClient.patch().uri(Endpoints.NOTIFICATION_BY_ID, id)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(NotificationDto.class);
    }

    @Override
    public List<NotificationDto> getUnreadNotificationsByUser(Long userId) {
        HalPage<NotificationDto> page = restClient.get().uri(Endpoints.NOTIFICATIONS_UNREAD_BY_USER, userId)
                .retrieve()
                .body(new ParameterizedTypeReference<HalPage<NotificationDto>>() {
                });
        return page.content();
    }

    @Override
    public UserProfileDto getUserProfile(Long userId) {
        return notFoundAsNull(() -> restClient.get().uri(Endpoints.QUERY_USER_PROFILE, userId)
                .retrieve()
                .body(UserProfileDto.class));
    }

    @Override
    public BookAvailabilityDto getBookAvailability(Long bookId) {
        return notFoundAsNull(() -> restClient.get().uri(Endpoints.QUERY_BOOK_AVAILABILITY, bookId)
                .retrieve()
                .body(BookAvailabilityDto.class));
    }

    @Override
    public List<UserBorrowingHistoryDto> getUserBorrowingHistory(Long userId) {
        return restClient.get().uri(Endpoints.QUERY_USER_BORROWING_HISTORY, userId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<UserBorrowingHistoryDto>>() {
                });
    }

    @Override
    public List<OverdueRecordDto> getOverdueRecords() {
        return restClient.get().uri(Endpoints.QUERY_OVERDUE_RECORDS)
                .retrieve()
                .body(new ParameterizedTypeReference<List<OverdueRecordDto>>() {
                });
    }
}
