package com.libraryapp.library.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads "Authorization: Bearer <jwt>", validates it, and - if valid - pushes an Authentication
 * into the security context so downstream authorizeHttpRequests() role checks can run.
 * Requests with no/invalid token simply proceed unauthenticated; SecurityConfig decides which
 * paths require authentication.
 * <p>
 * Deliberately NOT a @Component: it's constructed explicitly inside SecurityConfig and wired into
 * the security filter chain with addFilterBefore(). If it were also a @Component, Spring Boot's
 * auto-registration of Filter beans as a servlet container filter would register it a second time,
 * running it twice per request.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        if (!jwtService.isValid(token)) {
            chain.doFilter(request, response);
            return;
        }

        String username = jwtService.extractUsername(token);
        String role = jwtService.extractRole(token).name();
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

        var authentication = new UsernamePasswordAuthenticationToken(username, token, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Stash userId as a request attribute so controllers can enforce "own resource" checks
        // (e.g. a USER can only see their own loans) without re-parsing the token.
        request.setAttribute("userId", jwtService.extractUserId(token));
        request.setAttribute("username", username);
        request.setAttribute("role", role);

        chain.doFilter(request, response);
    }
}
