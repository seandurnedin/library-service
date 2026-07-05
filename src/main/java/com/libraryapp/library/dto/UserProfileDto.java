package com.libraryapp.library.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfileDto {

    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String status;
    private String role;
    private String userGroupName;
    private Integer maxBooksAllowed;
    private Integer loanDurationDays;
    private Integer activeLoanCount;
    private Integer activeReservationCount;
    private BigDecimal outstandingBalance;
}
