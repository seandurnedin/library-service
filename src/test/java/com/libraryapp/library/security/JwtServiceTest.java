package com.libraryapp.library.security;

import com.libraryapp.library.config.JwtProperties;
import com.libraryapp.library.enums.ERole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(jwtProperties(60));
    }

    private static JwtProperties jwtProperties(long expirationMinutes) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-must-be-at-least-32-bytes-long!!");
        properties.setExpirationMinutes(expirationMinutes);
        return properties;
    }

    @Test
    void generateToken_thenParse_roundTripsClaims() {
        String token = jwtService.generateToken(42L, "sean", ERole.MANAGER);

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("sean");
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        assertThat(jwtService.extractRole(token)).isEqualTo(ERole.MANAGER);
    }

    @Test
    void isValid_garbageToken_returnsFalse() {
        assertThat(jwtService.isValid("not-a-real-jwt")).isFalse();
    }

    @Test
    void isValid_expiredToken_returnsFalse() {
        JwtService shortLived = new JwtService(jwtProperties(0));
        String token = shortLived.generateToken(1L, "sean", ERole.USER);
        // 0-minute expiry means "now" - by the time we check, it's already expired.
        assertThat(shortLived.isValid(token)).isFalse();
    }

    @Test
    void expirationSeconds_matchesConfiguredMinutes() {
        assertThat(jwtService.expirationSeconds()).isEqualTo(3600L);
    }
}
