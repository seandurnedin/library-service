package com.libraryapp.library.controller;

import com.libraryapp.library.dto.AuthorisePaymentRequest;
import com.libraryapp.library.dto.PaymentDto;
import com.libraryapp.library.dto.PaymentRequest;
import com.libraryapp.library.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Payments", description = "Late fees, reservation fees")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Make a payment (mock - no real gateway is called)")
    @PostMapping("/pay")
    public PaymentDto pay(@Valid @RequestBody PaymentRequest request, HttpServletRequest servletRequest) {
        Long userId = (Long) servletRequest.getAttribute("userId");
        return paymentService.makePayment(userId, request);
    }

    @Operation(summary = "Clear a user's full outstanding balance by username, paying off every PENDING payment at once (MANAGER)")
    @PostMapping("/authorise")
    public List<PaymentDto> authorise(@Valid @RequestBody AuthorisePaymentRequest request) {
        return paymentService.authorisePayment(request.getUsername(), request.getAmount());
    }

    @Operation(summary = "View my payment history")
    @GetMapping("/history")
    public List<PaymentDto> history(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return paymentService.getPaymentHistory(userId);
    }
}
