package com.yusufziyrek.order.presentation.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtUserFilterTest {

    private static final String SECRET = "4fSI1eZ8v/Z49XUntoAPc474kR3JS0OKymkUa30Zd1I=";

    @Test
    void shouldSkipNonOrderPaths() throws ServletException, IOException {
        JwtUserFilter filter = new JwtUserFilter(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldReturnUnauthorizedWhenHeaderMissing() throws ServletException, IOException {
        JwtUserFilter filter = new JwtUserFilter(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    void shouldSetUserIdForValidToken() throws ServletException, IOException {
        JwtUserFilter filter = new JwtUserFilter(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String userId = UUID.randomUUID().toString();
        request.addHeader("Authorization", "Bearer " + token(userId));

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
        assertEquals(userId, request.getAttribute("user_id"));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIdClaimMissing() throws ServletException, IOException {
        JwtUserFilter filter = new JwtUserFilter(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader("Authorization", "Bearer " + tokenWithoutUserId());
        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        assertNull(request.getAttribute("user_id"));
    }

    private String token(String userId) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .claim("user_id", userId)
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(key)
                .compact();
    }

    private String tokenWithoutUserId() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .claim("email", "test@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(key)
                .compact();
    }
}
