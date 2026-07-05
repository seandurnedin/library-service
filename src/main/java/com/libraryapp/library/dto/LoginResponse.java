package com.libraryapp.library.dto;

import com.libraryapp.library.enums.ERole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private String token;
    private String tokenType;
    private Long userId;
    private String username;
    private ERole role;
    private Long expiresInSeconds;
}
