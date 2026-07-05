package com.libraryapp.library.controller;

import com.libraryapp.library.dto.UserBorrowingHistoryDto;
import com.libraryapp.library.dto.UserProfileDto;
import com.libraryapp.library.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Users", description = "Self-service profile and borrowing history")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;

    @Operation(summary = "My profile (active loans, reservations, balance)")
    @GetMapping("/me")
    public UserProfileDto me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return userProfileService.getProfile(userId);
    }

    @Operation(summary = "My full borrowing history")
    @GetMapping("/me/history")
    public List<UserBorrowingHistoryDto> myHistory(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return userProfileService.getBorrowingHistory(userId);
    }
}
