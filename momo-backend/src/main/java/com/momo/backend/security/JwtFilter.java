
package com.momo.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    // JWT 생성 및 검증을 담당하는 클래스
    private final TokenProvider tokenProvider;

    // 1. HTTP 요청이 들어오면 JWT 인증 처리
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 2. Authorization 요청 헤더 가져오기
        String header = request.getHeader(
                HttpHeaders.AUTHORIZATION);

        // 3. Bearer 방식의 JWT가 전달되었는지 확인
        if (header != null && header.startsWith("Bearer ")) {

            // Bearer 접두사를 제외하고 JWT 추출
            String token = header.substring(7);

            // 4. JWT 서명, 만료 여부와 Access Token 유형 검증
            if (tokenProvider.validateToken(token)) {

                // 5. 검증된 JWT로 로그인 사용자 인증 정보 설정
                SecurityContextHolder.getContext()
                        .setAuthentication(
                                tokenProvider.getAuthentication(token));
            }
        }

        // 6. 다음 Spring Security 필터로 요청 전달
        filterChain.doFilter(request, response);
    }
}
