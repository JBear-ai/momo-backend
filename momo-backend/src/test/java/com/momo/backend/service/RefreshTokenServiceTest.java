package com.momo.backend.service;

import com.momo.backend.dto.auth.TokenRefreshResponse;
import com.momo.backend.entity.RefreshToken;
import com.momo.backend.entity.User;
import com.momo.backend.exception.CustomException;
import com.momo.backend.repository.RefreshTokenRepository;
import com.momo.backend.repository.UserRepository;
import com.momo.backend.security.TokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import org.mockito.InOrder;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private User user;

    private TokenProvider tokenProvider;
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        tokenProvider = new TokenProvider(
                "TW9tb1Rlc3RPbmx5U2VjcmV0S2V5Rm9ySlZUVGVzdHMxMjM0NQ==",
                1_800_000L,
                604_800_000L);
        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository, userRepository, tokenProvider);
    }

    @Test
    void reissueRotatesBothTokensAndRejectsPreviousRefreshToken() {
        String loginId = "momo-user";
        Long userId = 1L;
        String oldRefreshToken = tokenProvider.createRefreshToken(loginId);
        RefreshToken storedToken = new RefreshToken(user, oldRefreshToken);

        when(user.getUserId()).thenReturn(userId);
        when(userRepository.findByLoginIdForUpdate(loginId)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(storedToken));

        TokenRefreshResponse response = refreshTokenService.reissue(oldRefreshToken);

        assertNotEquals(oldRefreshToken, response.refreshToken());
        assertNotEquals(response.refreshToken(), storedToken.getToken());
        assertEquals(64, storedToken.getToken().length());

        assertThrows(CustomException.class, () -> refreshTokenService.reissue(oldRefreshToken));

        // DB에는 해시만 저장되지만 새 Refresh Token 원문으로 다음 재발급이 가능하다.
        TokenRefreshResponse nextResponse = refreshTokenService.reissue(response.refreshToken());
        assertNotEquals(response.refreshToken(), nextResponse.refreshToken());
        assertEquals(64, storedToken.getToken().length());
    }

    @Test
    void mismatchedTokenDoesNotDeleteOrReplaceCurrentActiveToken() {
        String loginId = "momo-user";
        Long userId = 1L;
        String requestToken = tokenProvider.createRefreshToken(loginId);
        String activeToken = tokenProvider.createRefreshToken(loginId);
        RefreshToken storedToken = new RefreshToken(user, activeToken);

        when(user.getUserId()).thenReturn(userId);
        when(userRepository.findByLoginIdForUpdate(loginId)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(storedToken));

        CustomException error = assertThrows(
                CustomException.class,
                () -> refreshTokenService.reissue(requestToken));

        assertEquals("UNAUTHORIZED", error.getCode());
        assertEquals(activeToken, storedToken.getToken());
    }

    @Test
    void logoutLocksMemberBeforeDeletingItsRefreshToken() {
        when(user.getUserId()).thenReturn(1L);
        when(userRepository.findByLoginIdForUpdate("momo-user"))
                .thenReturn(Optional.of(user));

        refreshTokenService.deleteByLoginId("momo-user");

        InOrder order = inOrder(userRepository, refreshTokenRepository);
        order.verify(userRepository).findByLoginIdForUpdate("momo-user");
        order.verify(refreshTokenRepository).deleteByUser_UserId(1L);
    }

    @Test
    void accessTokenCannotBeUsedForReissue() {
        String accessToken = tokenProvider.createToken("momo-user");

        CustomException error = assertThrows(
                CustomException.class,
                () -> refreshTokenService.reissue(accessToken));

        assertEquals("UNAUTHORIZED", error.getCode());
    }

    @Test
    void refreshTokenWithoutStoredTokenIsRejected() {
        String loginId = "momo-user";
        Long userId = 1L;
        String refreshToken = tokenProvider.createRefreshToken(loginId);

        when(user.getUserId()).thenReturn(userId);
        when(userRepository.findByLoginIdForUpdate(loginId)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.empty());

        CustomException error = assertThrows(
                CustomException.class,
                () -> refreshTokenService.reissue(refreshToken));

        assertEquals("UNAUTHORIZED", error.getCode());
    }

    @Test
    void firstLoginLocksUserBeforeCreatingRefreshToken() {
        Long userId = 1L;
        String refreshToken = tokenProvider.createRefreshToken("momo-user");

        when(user.getUserId()).thenReturn(userId);
        when(userRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.empty());

        refreshTokenService.saveOrReplace(user, refreshToken);

        InOrder order = inOrder(userRepository, refreshTokenRepository);
        order.verify(userRepository).findByUserIdForUpdate(userId);
        order.verify(refreshTokenRepository).findByUserIdForUpdate(userId);
        order.verify(refreshTokenRepository).save(org.mockito.ArgumentMatchers.argThat(
                stored -> stored.getToken().length() == 64 && !stored.getToken().equals(refreshToken)));
    }
}
