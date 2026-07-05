package com.libraryapp.library.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class OverdueRecordDto {
    
    private Long borrowingRecordId;
    private Long userId;
    private String userEmail;
    private Long bookId;
    private String bookTitle;
    private LocalDate dueDate;
    private Long daysOverdue;
    private BigDecimal calculatedLateFee;
}
