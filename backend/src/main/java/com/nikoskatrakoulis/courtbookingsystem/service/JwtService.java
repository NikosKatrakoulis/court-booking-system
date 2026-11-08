package com.nikoskatrakoulis.courtbookingsystem.service;

import com.nikoskatrakoulis.courtbookingsystem.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration
    ) {
        if (secret == null || secret.getBytes().length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes long");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
