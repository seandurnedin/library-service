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
public class UserGroupDto {

    private Long id;
    private String name;
    private String description;
    private Integer maxBooksAllowed;
    private Integer loanDurationDays;
    private BigDecimal dailyLateFee;
}
