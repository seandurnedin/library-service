package com.libraryapp.library.service.impl;

import com.libraryapp.library.client.DomainClient;
import com.libraryapp.library.dto.LoginRequest;
import com.libraryapp.library.dto.LoginResponse;
import com.libraryapp.library.dto.RegisterRequest;
import com.libraryapp.library.dto.UserDto;
import com.libraryapp.library.dto.UserGroupDto;
import com.libraryapp.library.enums.ERole;
import com.libraryapp.library.enums.EUserStatus;
import com.libraryapp.library.exception.DuplicateResourceException;
import com.libraryapp.library.exception.InvalidCredentialsException;
import com.libraryapp.library.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceImplTest {

    @Mock
    private DomainClient domainClient;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private LoginServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .username("sean")
                .email("sean@example.com")
                .password("supersecret1")
                .fullName("Sean Tan")
                .build();

        loginRequest = LoginRequest.builder()
                .username("sean")
                .password("supersecret1")
                .build();
    }

    @Test
    void register_happyPath_createsUserAndReturnsToken() {
        when(domainClient.findUserByUsername("sean")).thenReturn(null);
        when(domainClient.findUserByEmail("sean@example.com")).thenReturn(null);
        when(passwordEncoder.encode("supersecret1")).thenReturn("hashed-pw");

        UserDto created = UserDto.builder()
                .id(1L).username("sean").email("sean@example.com")
                .fullName("Sean Tan").role(ERole.USER).status(EUserStatus.ACTIVE)
                .build();
        when(domainClient.createUser(any(UserDto.class))).thenReturn(created);
        when(jwtService.generateToken(1L, "sean", ERole.USER)).thenReturn("jwt-token");
        when(jwtService.expirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.register(registerRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getRole()).isEqualTo(ERole.USER);

        verify(passwordEncoder).encode("supersecret1");
        verify(domainClient).createUser(argThat(dto -> dto.getPasswordHash().equals("hashed-pw")));
    }

    @Test
    void register_noGroupSpecified_assignsDefaultUserGroup() {
        when(domainClient.findUserByUsername("sean")).thenReturn(null);
        when(domainClient.findUserByEmail("sean@example.com")).thenReturn(null);
        when(passwordEncoder.encode("supersecret1")).thenReturn("hashed-pw");
        when(domainClient.findUserGroupByName("USER")).thenReturn(UserGroupDto.builder().id(42L).name("USER").build());

        UserDto created = UserDto.builder()
                .id(1L).username("sean").role(ERole.USER).status(EUserStatus.ACTIVE).build();
        when(domainClient.createUser(any(UserDto.class))).thenReturn(created);
        when(jwtService.generateToken(1L, "sean", ERole.USER)).thenReturn("jwt-token");
        when(jwtService.expirationSeconds()).thenReturn(3600L);

        authService.register(registerRequest);

        verify(domainClient).createUser(argThat(dto -> dto.getUserGroupId() != null && dto.getUserGroupId().equals(42L)));
    }

    @Test
    void register_groupExplicitlySpecified_doesNotOverrideWithDefault() {
        registerRequest.setUserGroupId(7L);
        when(domainClient.findUserByUsername("sean")).thenReturn(null);
        when(domainClient.findUserByEmail("sean@example.com")).thenReturn(null);
        when(passwordEncoder.encode("supersecret1")).thenReturn("hashed-pw");

        UserDto created = UserDto.builder()
                .id(1L).username("sean").role(ERole.USER).status(EUserStatus.ACTIVE).build();
        when(domainClient.createUser(any(UserDto.class))).thenReturn(created);
        when(jwtService.generateToken(1L, "sean", ERole.USER)).thenReturn("jwt-token");
        when(jwtService.expirationSeconds()).thenReturn(3600L);

        authService.register(registerRequest);

        verify(domainClient, never()).findUserGroupByName(any());
        verify(domainClient).createUser(argThat(dto -> dto.getUserGroupId() != null && dto.getUserGroupId().equals(7L)));
    }

    @Test
    void register_duplicateUsername_throwsBeforeTouchingDomainService() {
        UserDto existing = UserDto.builder().id(5L).username("sean").build();
        when(domainClient.findUserByUsername("sean")).thenReturn(existing);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class);

        verify(domainClient, never()).createUser(any());
    }

    @Test
    void register_duplicateEmail_throws() {
        when(domainClient.findUserByUsername("sean")).thenReturn(null);
        UserDto existing = UserDto.builder().id(5L).email("sean@example.com").build();
        when(domainClient.findUserByEmail("sean@example.com")).thenReturn(existing);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class);

        verify(domainClient, never()).createUser(any());
    }

    @Test
    void login_correctCredentials_returnsToken() {
        UserDto user = UserDto.builder()
                .id(1L).username("sean").passwordHash("hashed-pw")
                .role(ERole.USER).status(EUserStatus.ACTIVE)
                .build();
        when(domainClient.findUserByUsername("sean")).thenReturn(user);
        when(passwordEncoder.matches("supersecret1", "hashed-pw")).thenReturn(true);
        when(jwtService.generateToken(1L, "sean", ERole.USER)).thenReturn("jwt-token");
        when(jwtService.expirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        UserDto user = UserDto.builder()
                .id(1L).username("sean").passwordHash("hashed-pw")
                .role(ERole.USER).status(EUserStatus.ACTIVE)
                .build();
        when(domainClient.findUserByUsername("sean")).thenReturn(user);
        when(passwordEncoder.matches("supersecret1", "hashed-pw")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_unknownUsername_throwsInvalidCredentials() {
        when(domainClient.findUserByUsername("sean")).thenReturn(null);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_suspendedAccount_throwsInvalidCredentials() {
        UserDto user = UserDto.builder()
                .id(1L).username("sean").passwordHash("hashed-pw")
                .role(ERole.USER).status(EUserStatus.SUSPENDED)
                .build();
        when(domainClient.findUserByUsername("sean")).thenReturn(user);
        when(passwordEncoder.matches("supersecret1", "hashed-pw")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
