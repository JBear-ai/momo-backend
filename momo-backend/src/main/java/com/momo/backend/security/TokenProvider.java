
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
import java.util.UUID;

@Slf4j
@Component
public class TokenProvider {

    // JWT 서명에 사용할 비밀키
    private final SecretKey key;

    // Access Token 유효기간 (밀리초)
    private final long accessExpMs;

    // Refresh Token 유효기간 (밀리초)
    private final long refreshExpMs;

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    // 1. JWT 비밀키와 만료시간 설정
    public TokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-exp-ms:1800000}") long accessExpMs,
            @Value("${jwt.refresh-exp-ms:604800000}") long refreshExpMs) {
        // Base64 문자열을 실제 비밀키로 변환
        this.key = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(secret));

        this.accessExpMs = accessExpMs;
        this.refreshExpMs = refreshExpMs;
    }

    // 2. 로그인 성공 시 Access Token 생성
    public String createToken(String loginId) {

        return createToken(loginId, ACCESS_TOKEN_TYPE, accessExpMs);
    }

    // 3. 로그인 및 재발급 성공 시 Refresh Token 생성
    public String createRefreshToken(String loginId) {

        return createToken(loginId, REFRESH_TOKEN_TYPE, refreshExpMs);
    }

    private String createToken(String loginId, String tokenType, long expirationMs) {

        // 토큰 발급 시간
        long now = System.currentTimeMillis();

        // 토큰 만료 시간
        Date expiration = new Date(now + expirationMs);

        // 토큰 유형을 명시하여 Refresh Token이 일반 API 인증에 사용되지 않도록 한다.
        return Jwts.builder()
                .subject(loginId)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(now))
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    // 4. Access Token의 서명, 만료 여부 및 토큰 유형 검증
    public boolean validateToken(String token) {

        return validateTokenType(token, ACCESS_TOKEN_TYPE);
    }

    // 5. Refresh Token의 서명, 만료 여부 및 토큰 유형 검증
    public boolean validateRefreshToken(String token) {

        return validateTokenType(token, REFRESH_TOKEN_TYPE);
    }

    private boolean validateTokenType(String token, String expectedType) {

        try {
            Claims claims = parseClaims(token);
            return expectedType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));

        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT입니다.");

        } catch (JwtException | IllegalArgumentException e) {
            log.info("유효하지 않은 JWT입니다.");
        }

        return false;
    }

    // 6. Access Token에서 로그인 사용자 인증 정보 생성
    public Authentication getAuthentication(String token) {

        // JWT에 저장된 사용자 정보 추출
        Claims claims = parseClaims(token);

        // MOMO는 loginId를 사용자 식별값으로 사용
        // Spring Security 연결
        String loginId = claims.getSubject();

        // 검증된 사용자의 인증 객체 생성
        return new UsernamePasswordAuthenticationToken(
                loginId,
                null,
                Collections.emptyList());
    }

    // 7. 검증된 JWT에서 로그인 아이디 추출
    public String getLoginId(String token) {
        return parseClaims(token).getSubject();
    }

    /** 검증된 Access Token의 만료 시각을 WebSocket 연결 수명 관리에 제공한다. */
    public java.time.Instant getExpiration(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    // 8. JWT 서명을 검증하고 저장된 정보 추출
    private Claims parseClaims(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
