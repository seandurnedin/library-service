package com.libraryapp.library.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "services.domain-service")
public class DomainServiceProperties {

    private String baseUrl;

    public static final class Resources {
        public static final String USERS = "users";
        public static final String BOOKS = "books";
        public static final String USER_GROUPS = "user-groups";
        public static final String BORROWING_RECORDS = "borrowing-records";

        private Resources() {
        }
    }

    // REST endpoint path templates
    public static final class Endpoints {
        public static final String USERS = "/api/users";
        public static final String USER_BY_ID = "/api/users/{id}";
        public static final String USERS_BY_USERNAME = "/api/users/search/by-username?username={username}";
        public static final String USERS_BY_EMAIL = "/api/users/search/by-email?email={email}";
        public static final String USERS_BY_ROLE = "/api/users/search/by-role?role={role}";

        public static final String USER_GROUPS = "/api/user-groups";
        public static final String USER_GROUP_BY_ID = "/api/user-groups/{id}";
        public static final String USER_GROUPS_BY_NAME = "/api/user-groups/search/by-name?name={name}";

        public static final String BOOKS = "/api/books";
        public static final String BOOK_BY_ID = "/api/books/{id}";
        public static final String BOOKS_BY_ISBN = "/api/books/search/by-isbn?isbn={isbn}";
        public static final String BOOKS_SEARCH_BY_TITLE = "/api/books/search/by-title";

        public static final String BORROWING_RECORDS = "/api/borrowing-records";
        public static final String BORROWING_RECORD_BY_ID = "/api/borrowing-records/{id}";
        public static final String BORROWING_RECORDS_BY_USER = "/api/borrowing-records/search/by-user?userId={userId}";
        public static final String BORROWING_RECORDS_BY_USER_AND_STATUS =
                "/api/borrowing-records/search/by-user-and-status?userId={userId}&status={status}";
        public static final String BORROWING_RECORDS_ACTIVE_FOR_BOOK =
                "/api/borrowing-records/search/active-for-book?bookId={bookId}&status=ON_LOAN,OVERDUE";

        public static final String RESERVATIONS = "/api/reservations";
        public static final String RESERVATION_BY_ID = "/api/reservations/{id}";
        public static final String RESERVATIONS_BY_USER = "/api/reservations/search/by-user?userId={userId}";
        public static final String RESERVATIONS_QUEUE_FOR_BOOK =
                "/api/reservations/search/queue-for-book?bookId={bookId}&status=RESERVED";
        public static final String RESERVATIONS_ACTIVE_FOR_BOOK =
                "/api/reservations/search/active-for-book?bookId={bookId}&status=RESERVED,NOTIFIED";

        public static final String PAYMENTS = "/api/payments";
        public static final String PAYMENT_BY_ID = "/api/payments/{id}";
        public static final String PAYMENTS_BY_USER = "/api/payments/search/by-user?userId={userId}";

        public static final String NOTIFICATIONS = "/api/notifications";
        public static final String NOTIFICATION_BY_ID = "/api/notifications/{id}";
        public static final String NOTIFICATIONS_UNREAD_BY_USER =
                "/api/notifications/search/unread-by-user?userId={userId}";

        public static final String QUERY_USER_PROFILE = "/query/users/{id}/profile";
        public static final String QUERY_BOOK_AVAILABILITY = "/query/books/{id}/availability";
        public static final String QUERY_USER_BORROWING_HISTORY = "/query/users/{id}/borrowing-history";
        public static final String QUERY_OVERDUE_RECORDS = "/query/borrowing-records/overdue";

        private Endpoints() {
        }
    }
}
