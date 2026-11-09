package com.momo.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;

@Slf4j
@Component
public class TokenProvider {

    private final SecretKey key;
    private final long accessExpMs;

    public TokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-exp-ms:1800000}") long accessExpMs
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT_SECRET 환경 변수가 필요합니다.");
        }

        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessExpMs = accessExpMs;
    }

    public String createToken(String loginId) {
        long now = System.currentTimeMillis();
        Date expiration = new Date(now + accessExpMs);

        return Jwts.builder()
                .subject(loginId)
                .issuedAt(new Date(now))
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT입니다.");
        } catch (JwtException | IllegalArgumentException e) {
            log.info("유효하지 않은 JWT입니다.");
        }

        return false;
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        String loginId = claims.getSubject();

        return new UsernamePasswordAuthenticationToken(
                loginId,
                null,
                Collections.emptyList()
        );
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
