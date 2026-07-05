package com.libraryapp.library.service;

import com.libraryapp.library.dto.LoginRequest;
import com.libraryapp.library.dto.LoginResponse;
import com.libraryapp.library.dto.RegisterRequest;

public interface LoginService {

    /**
     * Creates the account
     * Password is BCrypt-hashed here
     * Raw password never leaves this method.
     *
     */
    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}
