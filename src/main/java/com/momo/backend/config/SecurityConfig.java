package com.momo.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
                // 현재 개발 단계에서 API 테스트를 위해 임시 비활성화
                .csrf(csrf -> csrf.disable())

                .securityContext(context -> context.requireExplicitSave(true))

                // 로그인하지 않은 사용자도 접근할 수 있는 API 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/signup",
                                "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/api/movies/**",
                                "/api/boxoffice/**").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}