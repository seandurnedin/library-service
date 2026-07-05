package com.libraryapp.library.dto;

import com.libraryapp.library.enums.EPaymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    
    @NotNull
    private Long userId;
    private Long borrowingRecordId;
    @NotNull
    @Positive
    private BigDecimal amount;
    @NotNull
    private EPaymentType type;
}
