package com.libraryapp.library.controller;

import com.libraryapp.library.dto.LoginRequest;
import com.libraryapp.library.dto.LoginResponse;
import com.libraryapp.library.dto.RegisterRequest;
import com.libraryapp.library.service.LoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Account creation and login")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    @Operation(summary = "Create a new account")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        return loginService.register(request);
    }

    @Operation(summary = "Log in and receive a JWT")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return loginService.login(request);
    }
}
