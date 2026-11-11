
package com.momo.backend.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;

import com.momo.backend.repository.UserRepository;
import com.momo.backend.security.JwtFilter;
import com.momo.backend.security.TokenProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class SecurityConfig {

        @Value("${momo.cors.allowed-origins:http://localhost:3000,http://192.168.0.166:3000}")
        private String allowedOrigins;

        // 비밀번호 암호화 설정
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        // DB 회원 정보를 기준으로 사용자 인증 구성
        @Bean
        public UserDetailsService userDetailsService(UserRepository userRepository) {
                return loginId -> userRepository.findByLoginId(loginId)
                                .map(user -> org.springframework.security.core.userdetails.User
                                                .withUsername(user.getLoginId())
                                                .password(user.getPasswordHash())
                                                .authorities("USER")
                                                .build())
                                .orElseThrow(() -> new UsernameNotFoundException("회원 정보를 찾을 수 없습니다."));
        }

        // JWT 기반 무상태 인증 및 URL별 접근 권한 설정
        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        TokenProvider tokenProvider) throws Exception {

                http
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> {
                                })
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .formLogin(form -> form.disable())
                                .httpBasic(basic -> basic.disable())
                                .logout(logout -> logout.disable())
                                .authorizeHttpRequests(auth -> auth

                                                // Swagger 접근 허용
                                                .requestMatchers(
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/v3/api-docs/**")
                                                .permitAll()

                                                // 회원가입, 로그인, 토큰 재발급 허용
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/auth/signup",
                                                                "/api/auth/login",
                                                                "/api/auth/reissue")
                                                .permitAll()

                                                // 공개 회원 조회 및 프로필 이미지 조회
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/auth/exists/id/**",
                                                                "/api/auth/users/*/profile-image")
                                                .permitAll()

                                                // 로그아웃은 인증된 사용자만 허용
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/auth/logout")
                                                .authenticated()

                                                // 개인별 좋아요 영화 목록
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/movies/my/liked")
                                                .authenticated()

                                                // 공개 영화 조회
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/movies/**",
                                                                "/api/boxoffice/**")
                                                .permitAll()

                                                // 영화 포스터 일괄 조회
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/movies/posters/status")
                                                .permitAll()

                                                // 내 리뷰 및 좋아요한 리뷰는 개인 정보이므로 공개 규칙보다 먼저 인증을 요구한다.
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/reviews/me",
                                                                "/api/reviews/me/**")
                                                .authenticated()

                                                // 공개 리뷰, 댓글 및 반응 개수 조회
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/reviews",
                                                                "/api/reviews/*",
                                                                "/api/reviews/*/comments",
                                                                "/api/reviews/*/reactions",
                                                                "/api/reviews/movie/*",
                                                                "/api/reviews/movie/*/rating",
                                                                "/api/comments/*/reactions",
                                                                "/api/persons/**")
                                                .permitAll()

                                                // 그 외 API는 인증 필요
                                                .anyRequest().authenticated())

                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                                                        response.setContentType("application/json;charset=UTF-8");
                                                        response.getWriter().write(
                                                                        "{\"code\":\"UNAUTHORIZED\","
                                                                                        + "\"message\":\"로그인이 필요합니다.\"}");
                                                })
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        response.setStatus(HttpStatus.FORBIDDEN.value());
                                                        response.setContentType("application/json;charset=UTF-8");
                                                        response.getWriter().write(
                                                                        "{\"code\":\"FORBIDDEN\","
                                                                                        + "\"message\":\"접근 권한이 없습니다.\"}");
                                                }))

                                .addFilterBefore(
                                                new JwtFilter(tokenProvider),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        // React 개발 서버와의 API 통신 설정
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();

                config.setAllowedOrigins(
                                Arrays.stream(allowedOrigins.split(","))
                                                .map(origin -> origin == null ? "" : origin.trim())
                                                .filter(origin -> !origin.isEmpty())
                                                .distinct()
                                                .toList());
                config.setAllowedMethods(List.of(
                                "GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/api/**", config);

                return source;
        }
}
