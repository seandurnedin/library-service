package com.libraryapp.library.service.impl;

import com.libraryapp.library.client.DomainClient;
import com.libraryapp.library.dto.PaymentDto;
import com.libraryapp.library.dto.UserDto;
import com.libraryapp.library.enums.EPaymentStatus;
import com.libraryapp.library.exception.BusinessRuleException;
import com.libraryapp.library.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static final String USERNAME = "jdoe";

    @Mock
    private DomainClient domainClient;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void authorisePayment_exactAmount_marksAllPendingPaid() {
        UserDto user = UserDto.builder().id(1L).username(USERNAME).build();
        PaymentDto pending1 = PaymentDto.builder().id(100L).status(EPaymentStatus.PENDING).amount(new BigDecimal("2.50")).build();
        PaymentDto pending2 = PaymentDto.builder().id(101L).status(EPaymentStatus.PENDING).amount(new BigDecimal("1.00")).build();
        when(domainClient.findUserByUsername(USERNAME)).thenReturn(user);
        when(domainClient.getPaymentsByUser(1L)).thenReturn(List.of(pending1, pending2));
        when(domainClient.updatePayment(eq(100L), any(PaymentDto.class)))
                .thenReturn(PaymentDto.builder().id(100L).status(EPaymentStatus.PAID).build());
        when(domainClient.updatePayment(eq(101L), any(PaymentDto.class)))
                .thenReturn(PaymentDto.builder().id(101L).status(EPaymentStatus.PAID).build());

        List<PaymentDto> result = paymentService.authorisePayment(USERNAME, new BigDecimal("3.50"));

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(p -> p.getStatus() == EPaymentStatus.PAID);
    }

    @Test
    void authorisePayment_amountMismatch_throwsBusinessRule() {
        UserDto user = UserDto.builder().id(1L).username(USERNAME).build();
        PaymentDto pending = PaymentDto.builder().id(100L).status(EPaymentStatus.PENDING).amount(new BigDecimal("2.50")).build();
        when(domainClient.findUserByUsername(USERNAME)).thenReturn(user);
        when(domainClient.getPaymentsByUser(1L)).thenReturn(List.of(pending));

        assertThatThrownBy(() -> paymentService.authorisePayment(USERNAME, new BigDecimal("1.00")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void authorisePayment_noPendingPayments_throwsBusinessRule() {
        UserDto user = UserDto.builder().id(1L).username(USERNAME).build();
        when(domainClient.findUserByUsername(USERNAME)).thenReturn(user);
        when(domainClient.getPaymentsByUser(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> paymentService.authorisePayment(USERNAME, new BigDecimal("1.00")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void authorisePayment_userNotFound_throws() {
        when(domainClient.findUserByUsername(USERNAME)).thenReturn(null);

        assertThatThrownBy(() -> paymentService.authorisePayment(USERNAME, new BigDecimal("1.00")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
