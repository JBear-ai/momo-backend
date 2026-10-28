package com.momo.backend.controller;

import com.momo.backend.dto.auth.SignupRequest;
import com.momo.backend.dto.auth.UserResponse;
import com.momo.backend.service.AuthService;
import com.momo.backend.dto.auth.LoginRequest;
import com.momo.backend.entity.User;
import com.momo.backend.dto.auth.UserUpdateRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins="http://localhost:3000")
@Slf4j

public class AuthController {

    // 회원가입 기능을 처리하는 Service 객체
    private final AuthService authService;

    // 회원가입 요청을 받아 Service에 전달
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signUp(
            @Valid @RequestBody SignupRequest dto) {

        // 회원가입 처리 후 생성된 회원 정보를 반환
        UserResponse response = authService.signUp(dto);

        // 회원가입 성공 시 HTTP 201 응답
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // 로그인 요청을 받아 인증하고 세션에 로그인 상태 저장
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            @Valid @RequestBody LoginRequest dto,
            HttpServletRequest request) {

        // Service에서 아이디와 비밀번호 검증
        User user = authService.login(dto.getLoginId(), dto.getPassword());

        // 인증된 회원 정보를 Spring Security 인증 객체로 생성
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getLoginId(),
                        null,
                        Collections.emptyList()
                );

        // 현재 요청에 사용할 인증 정보 생성
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // 세션을 생성하거나 기존 세션을 가져옴
        HttpSession session = request.getSession();

        // 로그인 성공 후 세션 ID 변경
        request.changeSessionId();

        // Spring Security 인증 정보를 세션에 저장
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );

        // 로그인 성공 시 회원 정보 반환
        return ResponseEntity.ok(new UserResponse(user));
    }

    // 현재 세션에 로그인한 회원 정보 조회
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo(
            Authentication authentication) {

        // 세션에 저장된 로그인 아이디로 회원 조회
        User user = authService.getLoginUser(authentication.getName());

        return ResponseEntity.ok(new UserResponse(user));
    }


    // 현재 로그인한 회원의 닉네임과 이메일 수정
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyInfo(
            @Valid @RequestBody UserUpdateRequest dto,
            Authentication authentication) {

        UserResponse response = authService.updateMyInfo(
                authentication.getName(), dto);

        return ResponseEntity.ok(response);
    }

    // 현재 로그인한 회원 탈퇴 (탈퇴 시 리뷰 및 댓글 삭제)
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteMyAccount(
            Authentication authentication,
            HttpServletRequest request) {

        // 로그인한 회원의 계정 삭제
        authService.deleteMyAccount(authentication.getName());

        // 회원 탈퇴 후 세션 종료
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        // 현재 요청의 인증 정보 제거
        SecurityContextHolder.clearContext();

        // 회원 탈퇴 성공 메시지 반환
        return ResponseEntity.ok("회원 탈퇴가 완료되었습니다.");
    }



    // 로그아웃 시 현재 세션을 종료
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {

        // 현재 세션이 존재하면 무효화
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        // 현재 요청의 인증 정보 제거
        SecurityContextHolder.clearContext();

        return ResponseEntity.noContent().build();
    }
}