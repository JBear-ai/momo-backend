package com.momo.backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    // =========================================================
    // 1. 비밀번호 암호화 설정
    // =========================================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    // =========================================================
    // 2. SecurityContextRepository 설정
    // =========================================================

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }


    // =========================================================
    // 3. Spring Security 필터 설정
    // =========================================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository
    ) throws Exception {

        http

                // -------------------------------------------------
                // CSRF 비활성화
                // -------------------------------------------------
                .csrf(csrf -> csrf.disable())


                // -------------------------------------------------
                // CORS 활성화
                // -------------------------------------------------
                .cors(cors -> {})


                // -------------------------------------------------
                // SecurityContext 세션 저장 설정
                // -------------------------------------------------
                .securityContext(context -> context
                        .requireExplicitSave(true)
                        .securityContextRepository(securityContextRepository)
                )


                // -------------------------------------------------
                // URL별 접근 권한 설정
                // -------------------------------------------------
                .authorizeHttpRequests(auth -> auth

                        // Swagger 접근 허용
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // 회원가입 및 로그인 허용
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/signup",
                                "/api/auth/login"
                        ).permitAll()

                        // 아이디 중복 확인 허용
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/exists/id/**"
                        ).permitAll()

                        // 로그아웃은 로그인한 사용자만 허용
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/logout"
                        ).authenticated()

                        // 영화 조회 API 허용
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/movies/**",
                                "/api/boxoffice/**"
                        ).permitAll()

                        // 포스터 상태 조회 허용
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/movies/posters/status"
                        ).permitAll()

                        // 영화별 리뷰 및 평점 조회 허용
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/reviews/movie/*",
                                "/api/reviews/movie/*/rating"
                        ).permitAll()
                        // 인물 상세 조회 API 허용
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/persons/**"
                        ).permitAll()

                        // 그 외 API는 로그인 필요
                        .anyRequest().authenticated()
                )


                // -------------------------------------------------
                // 인증 실패 및 권한 거부 응답
                // -------------------------------------------------
                .exceptionHandling(exception -> exception

                        // 로그인하지 않은 사용자
                        .authenticationEntryPoint(
                                (request, response, authException) -> {

                                    response.setStatus(
                                            HttpStatus.UNAUTHORIZED.value()
                                    );

                                    response.setContentType(
                                            "application/json;charset=UTF-8"
                                    );

                                    response.getWriter().write(
                                            "{\"code\":\"UNAUTHORIZED\","
                                                    + "\"message\":\"로그인이 필요합니다.\"}"
                                    );
                                }
                        )

                        // 접근 권한이 없는 사용자
                        .accessDeniedHandler(
                                (request, response, accessDeniedException) -> {

                                    response.setStatus(
                                            HttpStatus.FORBIDDEN.value()
                                    );

                                    response.setContentType(
                                            "application/json;charset=UTF-8"
                                    );

                                    response.getWriter().write(
                                            "{\"code\":\"FORBIDDEN\","
                                                    + "\"message\":\"접근 권한이 없습니다.\"}"
                                    );
                                }
                        )
                );

        return http.build();
    }


    // =========================================================
    // 4. CORS 설정
    // =========================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        // React 개발 서버 주소
        config.setAllowedOrigins(
                List.of("http://localhost:3000")
        );

        // 허용할 HTTP 메서드
        config.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );

        // 모든 요청 헤더 허용
        config.setAllowedHeaders(
                List.of("*")
        );

        // 세션 쿠키 전송 허용
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/api/**",
                config
        );

        return source;
    }


}
