package com.libraryapp.library.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.libraryapp.library.enums.EReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReservationDto {

    private Long id;
    private Long userId;
    private String username;
    private Long bookId;
    private String bookTitle;
    private String isbn;
    private LocalDateTime reservationDate;
    private EReservationStatus status;
    private Integer queuePosition;
}
