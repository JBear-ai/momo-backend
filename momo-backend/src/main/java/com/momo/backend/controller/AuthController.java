package com.momo.backend.controller;

import com.momo.backend.dto.auth.PasswordCheckRequest;
import com.momo.backend.dto.auth.SignupRequest;
import com.momo.backend.dto.auth.UserResponse;
import com.momo.backend.dto.auth.LoginRequest;
import com.momo.backend.dto.auth.UserUpdateRequest;
import com.momo.backend.dto.auth.UserDeleteRequest;

import com.momo.backend.entity.User;
import com.momo.backend.service.AuthService;
import com.momo.backend.dto.auth.LoginResponse;
import com.momo.backend.security.TokenProvider;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

        private final AuthService authService;

        // 로그인 성공 시 Access Token을 발급한다.
        private final TokenProvider tokenProvider;

        // 1. 회원가입

        @PostMapping("/signup")
        public ResponseEntity<UserResponse> signUp(
                        @Valid @RequestBody SignupRequest dto) {

                UserResponse response = authService.signUp(dto);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(response);
        }

        // 2. 로그인 및 JWT 발급
        @PostMapping("/login")
        public ResponseEntity<LoginResponse> login(
                        @Valid @RequestBody LoginRequest dto) {

                User user = authService.login(
                                dto.getLoginId(),
                                dto.getPassword());

                String accessToken = tokenProvider.createToken(user.getLoginId());

                return ResponseEntity.ok(new LoginResponse(user, accessToken));
        }

        // 3. 로그인한 회원 정보 조회

        @GetMapping("/me")
        public ResponseEntity<UserResponse> getMyInfo(
                        Authentication authentication) {

                User user = authService.getLoginUser(
                                authentication.getName());

                return ResponseEntity.ok(new UserResponse(user));
        }

        // 4. 회원 정보 수정

        @PutMapping("/me")
        public ResponseEntity<UserResponse> updateMyInfo(
                        @Valid @RequestBody UserUpdateRequest dto,
                        Authentication authentication) {

                UserResponse response = authService.updateMyInfo(
                                authentication.getName(),
                                dto);

                return ResponseEntity.ok(response);
        }

        // 5. 회원 탈퇴

        @DeleteMapping("/me")
        public ResponseEntity<String> deleteMyAccount(
                        @Valid @RequestBody UserDeleteRequest dto,
                        Authentication authentication) {

                // 회원 탈퇴 처리
                authService.deleteMyAccount(
                                authentication.getName(),
                                dto.password());

                // 인증 정보 제거
                SecurityContextHolder.clearContext();

                return ResponseEntity.ok("회원 탈퇴가 완료되었습니다.");
        }

        // 6. 로그아웃
        @PostMapping("/logout")
        public ResponseEntity<Void> logout() {
                // JWT는 서버 세션에 저장하지 않으므로 클라이언트에서 토큰을 삭제한다.
                SecurityContextHolder.clearContext();
                return ResponseEntity.noContent().build();
        }

        // 7. 내가 누른 좋아요 총개수 조회

        @GetMapping("/me/likes/count")
        public ResponseEntity<Long> getMyLikeCount(
                        Authentication authentication) {

                long count = authService.getMyLikeCount(
                                authentication.getName());

                return ResponseEntity.ok(count);
        }

        // 8. 회원 아이디 중복 확인

        @GetMapping("/exists/id/{loginId}")
        public ResponseEntity<Boolean> checkLoginId(
                        @PathVariable String loginId) {

                boolean exists = authService.isMember(loginId);

                return ResponseEntity.ok(exists);
        }

        // 9. 현재 비밀번호 일치 여부 확인

        @PostMapping("/me/password-check")
        public ResponseEntity<Boolean> checkCurrentPassword(
                        @Valid @RequestBody PasswordCheckRequest dto,
                        Authentication authentication) {

                boolean result = authService.checkCurrentPassword(
                                authentication.getName(),
                                dto.currentPassword());

                return ResponseEntity.ok(result);
        }

        // 10. 프로필 이미지 업로드 및 변경
        @PostMapping("/me/profile-image")
        public ResponseEntity<UserResponse> updateProfileImage(
                        @RequestParam("file") MultipartFile file,
                        Authentication authentication) throws Exception {

                // 로그인한 회원의 프로필 이미지를 Firebase Storage에 업로드
                UserResponse response = authService.updateProfileImage(
                                authentication.getName(),
                                file);

                return ResponseEntity.ok(response);
        }

        // 11. 프로필 이미지 삭제
        @DeleteMapping("/me/profile-image")
        public ResponseEntity<UserResponse> removeProfileImage(
                        Authentication authentication) {

                // 로그인한 회원의 프로필 이미지 정보를 제거
                UserResponse response = authService.removeProfileImage(
                                authentication.getName());

                return ResponseEntity.ok(response);
        }

        // 12. 프로필 이미지 조회
        @GetMapping("/me/profile-image")
        public ResponseEntity<byte[]> getProfileImage(
                        Authentication authentication) {

                String loginId = authentication.getName();

                byte[] image = authService.getProfileImage(loginId);

                if (image == null || image.length == 0) {
                        return ResponseEntity.notFound().build();
                }

                String contentType = authService.getProfileImageContentType(loginId);

                if (contentType == null) {
                        return ResponseEntity.notFound().build();
                }

                return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(contentType))
                                .body(image);
        }

}
