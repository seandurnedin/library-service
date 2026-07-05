package com.libraryapp.library.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.libraryapp.library.enums.EBorrowingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BorrowingRecordDto {

    private Long id;
    private Long userId;
    private Long bookId;
    private String bookTitle;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private EBorrowingStatus status;
    private BigDecimal lateFee;
}
