package com.libraryapp.library.service.impl;

import com.libraryapp.library.client.DomainClient;
import com.libraryapp.library.dto.*;
import com.libraryapp.library.enums.ERole;
import com.libraryapp.library.enums.EUserStatus;
import com.libraryapp.library.exception.DuplicateResourceException;
import com.libraryapp.library.exception.InvalidCredentialsException;
import com.libraryapp.library.security.JwtService;
import com.libraryapp.library.service.LoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private static final String DEFAULT_USER_GROUP_NAME = "USER";

    private final DomainClient domainClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public LoginResponse register(RegisterRequest request) {
        if (domainClient.findUserByUsername(request.getUsername()) != null) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }
        if (domainClient.findUserByEmail(request.getEmail()) != null) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }
        return createAccount(request);
    }

    private LoginResponse createAccount(RegisterRequest request) {
        Long userGroupId = request.getUserGroupId() != null ? request.getUserGroupId() : defaultUserGroupId();

        UserDto newUser = UserDto.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .status(EUserStatus.ACTIVE)
                .role(ERole.USER)
                .userGroupId(userGroupId)
                .build();

        UserDto created = domainClient.createUser(newUser);
        if (created == null) {
            created = domainClient.findUserByUsername(request.getUsername());
        }
        return issueToken(created);
    }

    private Long defaultUserGroupId() {
        UserGroupDto defaultGroup = domainClient.findUserGroupByName(DEFAULT_USER_GROUP_NAME);
        if (defaultGroup == null) {
            log.warn("Default user group \"{}\" not found - registering user with no group", DEFAULT_USER_GROUP_NAME);
            return null;
        }
        return defaultGroup.getId();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        UserDto user = domainClient.findUserByUsername(request.getUsername());
        if (user == null) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
        if (user.getStatus() == EUserStatus.SUSPENDED || user.getStatus() == EUserStatus.INACTIVE) {
            throw new InvalidCredentialsException("This account is " + user.getStatus().getValue().toLowerCase());
        }
        return issueToken(user);
    }

    private LoginResponse issueToken(UserDto user) {
        String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole());
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .expiresInSeconds(jwtService.expirationSeconds())
                .build();
    }
}
