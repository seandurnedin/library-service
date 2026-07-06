package com.libraryapp.library.service.impl;

import com.libraryapp.library.client.DomainClient;
import com.libraryapp.library.dto.PaymentDto;
import com.libraryapp.library.dto.PaymentRequest;
import com.libraryapp.library.dto.UserDto;
import com.libraryapp.library.enums.EPaymentStatus;
import com.libraryapp.library.exception.BusinessRuleException;
import com.libraryapp.library.exception.ResourceNotFoundException;
import com.libraryapp.library.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final DomainClient domainClient;

    @Override
    public PaymentDto makePayment(Long userId, PaymentRequest request) {

        BigDecimal outstanding = domainClient.getPaymentsByUser(userId).stream()
                .filter(p -> p.getStatus() == EPaymentStatus.PENDING)
                .map(PaymentDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("You have no outstanding balance to pay");
        }

        BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(outstanding) > 0) {
            throw new BusinessRuleException(
                    "Payment (" + amount + ") exceeds your outstanding balance (" + outstanding + ")");
        }

        PaymentDto newPayment = PaymentDto.builder()
                .amount(request.getAmount())
                .type(request.getType())
                .status(EPaymentStatus.PENDING)
                .build();
        
        PaymentDto created = domainClient.createPayment(userId, request.getBorrowingRecordId(), newPayment);
        return domainClient.updatePayment(created.getId(), PaymentDto.builder().status(EPaymentStatus.PAID).build());
    }

    @Override
    public List<PaymentDto> authorisePayment(String username, BigDecimal amountPaid) {
        UserDto user = domainClient.findUserByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("User not found: " + username);
        }

        List<PaymentDto> pending = domainClient.getPaymentsByUser(user.getId()).stream()
                .filter(p -> p.getStatus() == EPaymentStatus.PENDING)
                .toList();
        if (pending.isEmpty()) {
            throw new BusinessRuleException("\"" + username + "\" has no pending payments to authorise");
        }

        BigDecimal outstanding = pending.stream()
                .map(PaymentDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal tendered = amountPaid.setScale(2, RoundingMode.HALF_UP);
        if (tendered.compareTo(outstanding) != 0) {
            throw new BusinessRuleException(
                    "Amount paid (" + tendered + ") does not match \"" + username + "\"'s outstanding balance (" + outstanding + ")");
        }

        return pending.stream()
                .map(p -> domainClient.updatePayment(p.getId(), PaymentDto.builder().status(EPaymentStatus.PAID).build()))
                .toList();
    }

    @Override
    public List<PaymentDto> getPaymentHistory(Long userId) {
        return domainClient.getPaymentsByUser(userId);
    }
}
