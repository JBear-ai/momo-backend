package com.momo.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;
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
                // 개발 단계에서 Postman API 테스트를 위해 CSRF 보호 기능 임시 비활성화
                .csrf(csrf -> csrf.disable())

                // React에서 Spring Boot API 호출 허용
                .cors(cors -> {})

                // 로그인 인증 정보를 자동 저장하지 않고 Controller에서 직접 세션에 저장
                .securityContext(context -> context.requireExplicitSave(true))

                // API 주소별로 로그인 여부에 따른 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 개발 중 Swagger API 문서 접근 허용
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // 회원가입과 로그인은 로그인하지 않은 사용자도 접근 가능
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/signup",
                                "/api/auth/login").permitAll()
                        // 아이디 중복 확인은 로그인 없이 접근 허용 (11/02 추가)
                        .requestMatchers(HttpMethod.GET,
                                "/api/auth/exists/id/**").permitAll()
                        // 로그아웃은 로그인한 사용자만 요청 가능
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/logout").authenticated()

                                // 영화 검색과 상세정보, 박스오피스 조회는 로그인 없이 접근 가능
                                .requestMatchers(HttpMethod.GET,
                                        "/api/movies/**",
                                        "/api/boxoffice/**").permitAll()

                                // 영화별 리뷰 목록과 좋아요/싫어요 통계는 로그인 없이 조회 가능
                                .requestMatchers(HttpMethod.GET,
                                        "/api/reviews/movie/*",
                                        "/api/reviews/movie/*/rating").permitAll()

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
    // React 개발 서버에서 백엔드 API 접근 허용
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        // 로그인 세션 쿠키를 포함한 요청 허용
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/**", config);

        return source;
    }

}