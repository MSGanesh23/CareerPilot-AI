package com.careerpilot.security;

import com.careerpilot.config.AppProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final AppProperties appProperties;

    // ----------------------------------------------------------------
    // Generate access token from Authentication object
    // ----------------------------------------------------------------
    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return buildToken(userPrincipal, appProperties.getJwt().getExpirationMs());
    }

    // ----------------------------------------------------------------
    // Generate access token directly from UserPrincipal
    // ----------------------------------------------------------------
    public String generateAccessTokenFromPrincipal(UserPrincipal principal) {
        return buildToken(principal, appProperties.getJwt().getExpirationMs());
    }

    // ----------------------------------------------------------------
    // Build token
    // ----------------------------------------------------------------
    private String buildToken(UserPrincipal principal, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        String roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(String.valueOf(principal.getId()))
                .claim("email", principal.getEmail())
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    // ----------------------------------------------------------------
    // Extract user ID from token
    // ----------------------------------------------------------------
    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    // ----------------------------------------------------------------
    // Validate token
    // ----------------------------------------------------------------
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        // JWT_SECRET must be a plain-text string of at least 32 characters.
        // We use its raw UTF-8 bytes as the HMAC-SHA256 key material.
        // In production, set JWT_SECRET to a cryptographically random 64+ char string.
        byte[] keyBytes = appProperties.getJwt().getSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
