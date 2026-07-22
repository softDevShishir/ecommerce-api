package com.shishir.ecommerce.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * Generates and inspects JWTs signed with HS512. The signing key is derived by
 * SHA-512 hashing the configured secret into a fixed 64 bytes, so HS512's
 * 512-bit minimum is always satisfied regardless of the secret's length.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(sha512(secret));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email, String userId, UserRole role) {
        try {
            Date now = new Date();
            Date expiry = new Date(now.getTime() + expirationMs);

            return Jwts.builder()
                    .subject(email)
                    .claim("userId", userId)
                    .claim("role", role.name())
                    .claim("email", email)
                    .issuedAt(now)
                    .expiration(expiry)
                    .signWith(key, Jwts.SIG.HS512)
                    .compact();
        } catch (JwtException ex) {
            log.error("Failed to generate JWT for {}: {}", email, ex.getMessage());
            throw ex;
        }
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getUserIdFromToken(String token) {
        return parseClaims(token).get("userId", String.class);
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    // Seconds remaining until expiry; negative if the token has already expired.
    public Long getExpirationTimeFromToken(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return (expiration.getTime() - System.currentTimeMillis()) / 1000;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static byte[] sha512(String secret) {
        try {
            return MessageDigest.getInstance("SHA-512")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-512 not available", ex);
        }
    }
}
