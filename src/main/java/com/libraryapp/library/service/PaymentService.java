package com.libraryapp.library.service;

import com.libraryapp.library.dto.PaymentDto;
import com.libraryapp.library.dto.PaymentRequest;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentService {

    PaymentDto makePayment(Long userId, PaymentRequest request);

    List<PaymentDto> authorisePayment(String username, BigDecimal amountPaid);

    List<PaymentDto> getPaymentHistory(Long userId);
}
