package com.libraryapp.library.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.libraryapp.library.enums.EPaymentStatus;
import com.libraryapp.library.enums.EPaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentDto {
    
    private Long id;
    private Long userId;
    private Long borrowingRecordId;
    private BigDecimal amount;
    private EPaymentType type;
    private EPaymentStatus status;
    private LocalDateTime paymentDate;
}
