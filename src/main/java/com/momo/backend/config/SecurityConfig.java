package com.momo.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpStatus;

@Configuration
public class SecurityConfig {

    // 회원 비밀번호를 BCrypt 방식으로 암호화
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
                // 개발 단계에서 Postman API 테스트를 위해 CSRF 보호 기능 임시 비활성화
                .csrf(csrf -> csrf.disable())

                // 로그인 인증 정보를 자동 저장하지 않고 Controller에서 직접 세션에 저장
                .securityContext(context -> context.requireExplicitSave(true))

                // API 주소별로 로그인 여부에 따른 접근 권한 설정
                .authorizeHttpRequests(auth -> auth

                        // 회원가입과 로그인은 로그인하지 않은 사용자도 접근 가능
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/signup",
                                "/api/auth/login").permitAll()

                        // 로그아웃은 로그인한 사용자만 요청 가능
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/logout").authenticated()

                        // 영화 검색과 상세정보, 박스오피스 조회는 로그인 없이 접근 가능
                        .requestMatchers(HttpMethod.GET,
                                "/api/movies/**",
                                "/api/boxoffice/**").permitAll()

                        // 위에서 허용한 API를 제외한 나머지는 로그인 필요
                        .anyRequest().authenticated()
                )

                // 로그인하지 않은 사용자가 인증이 필요한 API에 접근했을 때 처리
                .exceptionHandling(exception -> exception

                        // 인증 정보가 없으면 HTTP 401 상태 코드 반환
                        .authenticationEntryPoint((request, response, authException) ->
                        {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        })
                );
        return http.build();
    }
}