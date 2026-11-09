<<<<<<< HEAD

=======
>>>>>>> cc695ef64281b4668a67faa322b58ba90af5d552
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

<<<<<<< HEAD
    // JWT 서명에 사용할 비밀키
    private final SecretKey key;

    // Access Token 유효기간 (밀리초)
    private final long accessExpMs;

    // 1. JWT 비밀키와 만료시간 설정
    public TokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-exp-ms:1800000}") long accessExpMs)
    {
        // Base64 문자열을 실제 비밀키로 변환
        this.key = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(secret)
        );

        this.accessExpMs = accessExpMs;
    }

    // 2. 로그인 성공 시 Access Token 생성
    public String createToken(String loginId) {

        // 토큰 발급 시간
        long now = System.currentTimeMillis();

        // 토큰 만료 시간
        Date expiration = new Date(now + accessExpMs);

        // 로그인 아이디와 만료시간을 포함한 JWT 생성
=======
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

>>>>>>> cc695ef64281b4668a67faa322b58ba90af5d552
        return Jwts.builder()
                .subject(loginId)
                .issuedAt(new Date(now))
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

<<<<<<< HEAD
    // 3. 전달받은 JWT의 서명과 만료 여부 검증
    public boolean validateToken(String token) {

        try {
            parseClaims(token);
            return true;

        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT입니다.");

=======
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT입니다.");
>>>>>>> cc695ef64281b4668a67faa322b58ba90af5d552
        } catch (JwtException | IllegalArgumentException e) {
            log.info("유효하지 않은 JWT입니다.");
        }

        return false;
    }

<<<<<<< HEAD
    // 4. JWT에서 로그인 사용자 인증 정보 생성
    public Authentication getAuthentication(String token) {

        // JWT에 저장된 사용자 정보 추출
        Claims claims = parseClaims(token);

        // MOMO는 loginId를 사용자 식별값으로 사용
        // Spring Security 연결
        String loginId = claims.getSubject();

        // 검증된 사용자의 인증 객체 생성
=======
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        String loginId = claims.getSubject();

>>>>>>> cc695ef64281b4668a67faa322b58ba90af5d552
        return new UsernamePasswordAuthenticationToken(
                loginId,
                null,
                Collections.emptyList()
        );
    }

<<<<<<< HEAD
    // 5. JWT 서명을 검증하고 저장된 정보 추출
    private Claims parseClaims(String token) {

=======
    private Claims parseClaims(String token) {
>>>>>>> cc695ef64281b4668a67faa322b58ba90af5d552
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
