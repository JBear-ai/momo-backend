package com.momo.backend.service;

import com.momo.backend.dto.auth.TokenRefreshResponse;
import com.momo.backend.entity.RefreshToken;
import com.momo.backend.entity.User;
import com.momo.backend.exception.CustomException;
import com.momo.backend.repository.RefreshTokenRepository;
import com.momo.backend.repository.UserRepository;
import com.momo.backend.security.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

// Refresh Token의 저장, Rotation 및 폐기를 담당한다.
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;

    public void saveOrReplace(User user, String token) {
        // 항상 존재하는 사용자 행을 먼저 잠가 Refresh Token 최초 INSERT 충돌을 방지한다.
        User lockedUser = userRepository.findByUserIdForUpdate(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        Optional<RefreshToken> storedToken = refreshTokenRepository.findByUserIdForUpdate(lockedUser.getUserId());

        if (storedToken.isPresent()) {
            storedToken.get().rotate(token);
            return;
        }

        refreshTokenRepository.save(new RefreshToken(lockedUser, token));
    }

    // 검증과 DB 교체를 한 트랜잭션에서 수행하여 Rotation의 원자성을 보장한다.
    public TokenRefreshResponse reissue(String requestToken) {
        if (!tokenProvider.validateRefreshToken(requestToken)) {
            throw unauthorized("유효하지 않은 Refresh Token입니다.");
        }

        String loginId = tokenProvider.getLoginId(requestToken);
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> unauthorized("Refresh Token의 회원 정보를 찾을 수 없습니다."));

        RefreshToken storedToken = refreshTokenRepository.findByUserIdForUpdate(user.getUserId())
                .orElseThrow(() -> unauthorized("저장된 Refresh Token이 없습니다."));

        if (!tokensMatch(requestToken, storedToken.getToken())) {
            // 불일치만으로 탈취를 확정할 수 없으므로 다른 로그인 세션의 활성 토큰은 보존한다.
            throw unauthorized("Refresh Token이 일치하지 않습니다.");
        }

        String newAccessToken = tokenProvider.createToken(loginId);
        String newRefreshToken = tokenProvider.createRefreshToken(loginId);
        storedToken.rotate(newRefreshToken);

        return new TokenRefreshResponse(newAccessToken, newRefreshToken);
    }

    public void deleteByLoginId(String loginId) {
        refreshTokenRepository.deleteByUser_LoginId(loginId);
    }

    public void deleteByUserId(Long userId) {
        refreshTokenRepository.deleteByUser_UserId(userId);
    }

    private boolean tokensMatch(String requestToken, String storedToken) {
        return MessageDigest.isEqual(
                requestToken.getBytes(StandardCharsets.UTF_8),
                storedToken.getBytes(StandardCharsets.UTF_8));
    }

    private CustomException unauthorized(String message) {
        return new CustomException("UNAUTHORIZED", message);
    }
}
