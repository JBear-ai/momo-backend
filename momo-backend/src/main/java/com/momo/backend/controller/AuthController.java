package com.momo.backend.controller;

import com.momo.backend.dto.auth.PasswordCheckRequest;
import com.momo.backend.dto.auth.SignupRequest;
import com.momo.backend.dto.auth.UserResponse;
import com.momo.backend.dto.auth.LoginRequest;
import com.momo.backend.dto.auth.UserUpdateRequest;
import com.momo.backend.dto.auth.UserDeleteRequest;

import com.momo.backend.entity.User;
import com.momo.backend.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    // Spring Security 인증 정보를 세션에 저장하는 저장소
    private final SecurityContextRepository securityContextRepository;


    // =========================================================
    // 1. 회원가입
    // =========================================================

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signUp(
            @Valid @RequestBody SignupRequest dto) {

        UserResponse response = authService.signUp(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // =========================================================
    // 2. 로그인
    // =========================================================

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            @Valid @RequestBody LoginRequest dto,
            HttpServletRequest request,
            HttpServletResponse response) {

        // 1. 회원 인증
        User user = authService.login(
                dto.getLoginId(),
                dto.getPassword()
        );

        // 2. 인증 객체 생성
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getLoginId(),
                        null,
                        Collections.emptyList()
                );

        // 3. SecurityContext 생성
        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);

        // 4. 현재 요청의 인증 정보 설정
        SecurityContextHolder.setContext(context);

        // 5. 세션 생성 및 세션 ID 변경
        request.getSession(true);
        request.changeSessionId();

        // 6. SecurityContext를 세션에 저장
        securityContextRepository.saveContext(
                context,
                request,
                response
        );

        return ResponseEntity.ok(new UserResponse(user));
    }


    // =========================================================
    // 3. 로그인한 회원 정보 조회
    // =========================================================

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo(
            Authentication authentication) {

        User user = authService.getLoginUser(
                authentication.getName()
        );

        return ResponseEntity.ok(new UserResponse(user));
    }


    // =========================================================
    // 4. 회원 정보 수정
    // =========================================================

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyInfo(
            @Valid @RequestBody UserUpdateRequest dto,
            Authentication authentication) {

        UserResponse response = authService.updateMyInfo(
                authentication.getName(),
                dto
        );

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // 5. 회원 탈퇴
    // =========================================================

    @DeleteMapping("/me")
    public ResponseEntity<String> deleteMyAccount(
            @Valid @RequestBody UserDeleteRequest dto,
            Authentication authentication,
            HttpServletRequest request) {

        // 회원 탈퇴 처리
        authService.deleteMyAccount(
                authentication.getName(),
                dto.password()
        );

        // 세션 종료
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        // 인증 정보 제거
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok("회원 탈퇴가 완료되었습니다.");
    }


    // =========================================================
    // 6. 로그아웃
    // =========================================================

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request) {

        // 현재 세션 조회
        HttpSession session = request.getSession(false);

        // 세션 종료
        if (session != null) {
            session.invalidate();
        }

        // 인증 정보 제거
        SecurityContextHolder.clearContext();

        return ResponseEntity.noContent().build();
    }


    // =========================================================
    // 7. 내가 누른 좋아요 총개수 조회
    // =========================================================

    @GetMapping("/me/likes/count")
    public ResponseEntity<Long> getMyLikeCount(
            Authentication authentication) {

        long count = authService.getMyLikeCount(
                authentication.getName()
        );

        return ResponseEntity.ok(count);
    }


    // =========================================================
    // 8. 회원 아이디 중복 확인
    // =========================================================

    @GetMapping("/exists/id/{loginId}")
    public ResponseEntity<Boolean> checkLoginId(
            @PathVariable String loginId) {

        boolean exists = authService.isMember(loginId);

        return ResponseEntity.ok(exists);
    }


    // =========================================================
    // 9. 현재 비밀번호 일치 여부 확인
    // =========================================================

    @PostMapping("/me/password-check")
    public ResponseEntity<Boolean> checkCurrentPassword(
            @Valid @RequestBody PasswordCheckRequest dto,
            Authentication authentication) {

        boolean result = authService.checkCurrentPassword(
                authentication.getName(),
                dto.currentPassword()
        );

        return ResponseEntity.ok(result);
    }

    // =========================================================
    // 10. 프로필 이미지 업로드 및 변경
    // =========================================================
    @PostMapping("/me/profile-image")
    public ResponseEntity<UserResponse> updateProfileImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws Exception {

        // 로그인한 회원의 프로필 이미지를 Firebase Storage에 업로드
        UserResponse response = authService.updateProfileImage(
                authentication.getName(),
                file
        );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // 11. 프로필 이미지 삭제
    // =========================================================
    @DeleteMapping("/me/profile-image")
    public ResponseEntity<UserResponse> removeProfileImage(
            Authentication authentication) {

        // 로그인한 회원의 프로필 이미지 정보를 제거
        UserResponse response = authService.removeProfileImage(
                authentication.getName()
        );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // 12. 프로필 이미지 조회
    // =========================================================

    @GetMapping("/me/profile-image")
    public ResponseEntity<byte[]> getProfileImage(
            Authentication authentication) {

        // 로그인한 회원의 프로필 이미지 데이터를 조회
        byte[] image = authService.getProfileImage(
                authentication.getName()
        );

        // 저장된 프로필 이미지가 없는 경우 404 응답
        if (image == null) {
            return ResponseEntity.notFound().build();
        }

        // 저장된 이미지가 JPG인지 PNG인지 확인합니다.
        String contentType = authService.getProfileImageContentType(
                authentication.getName()
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(image);
    }
}