package com.libraryapp.library.controller;

import com.libraryapp.library.dto.BorrowingRecordDto;
import com.libraryapp.library.dto.LoanRequest;
import com.libraryapp.library.dto.ReturnRequest;
import com.libraryapp.library.service.LendingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Lending", description = "Checking books in and out at the lending desk")
@RestController
@RequestMapping("/api/lending")
@RequiredArgsConstructor
public class LendingController {

    private final LendingService lendingService;

    @Operation(summary = "Check a book out to a member, identified by username and book ISBN (MANAGER)")
    @PostMapping("/loan")
    public BorrowingRecordDto loanBook(@Valid @RequestBody LoanRequest request) {
        return lendingService.loanBook(request.getUsername(), request.getIsbn());
    }

    @Operation(summary = "Check a book back in by username and book ISBN, applying any late fee (MANAGER)")
    @PostMapping("/return")
    public BorrowingRecordDto returnBook(@Valid @RequestBody ReturnRequest request) {
        return lendingService.returnBook(request.getUsername(), request.getIsbn());
    }

    @Operation(summary = "View my currently loaned books")
    @GetMapping("/my-loans")
    public List<BorrowingRecordDto> myLoans(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return lendingService.getLoanedBooks(userId);
    }
}
