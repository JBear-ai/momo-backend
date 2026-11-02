
package com.momo.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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
                // 개발 단계에서 CSRF 보호 기능 비활성화
                .csrf(csrf -> csrf.disable())

                // React에서 Spring Boot API 호출 허용
                .cors(cors -> {})

                // 인증 정보를 Controller에서 직접 세션에 저장
                .securityContext(context -> context.requireExplicitSave(true))

                // API별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth

                        // Swagger 접근 허용
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // 회원가입과 로그인
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/signup",
                                "/api/auth/login"
                        ).permitAll()

                        // 아이디 중복 확인
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/exists/id/**"
                        ).permitAll()

                        // 로그아웃
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/logout"
                        ).authenticated()

                        // 영화 검색, 상세정보, 박스오피스 조회
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/movies/**",
                                "/api/boxoffice/**"
                        ).permitAll()

                        // 포스터 상태 일괄 조회
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/movies/posters/status"
                        ).permitAll()

                        // 영화별 리뷰 목록과 좋아요/싫어요 통계
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/reviews/movie/*",
                                "/api/reviews/movie/*/rating"
                        ).permitAll()

                        // 그 외 API는 로그인 필요
                        .anyRequest().authenticated()
                )

                // 인증되지 않은 요청은 401 반환
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        })
                );

        return http.build();
    }

    // React 개발 서버 CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );
        config.setAllowedHeaders(List.of("*"));

        // 로그인 세션 쿠키를 포함한 요청 허용
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/**", config);

        return source;
    }
}
