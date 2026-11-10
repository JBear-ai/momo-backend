package com.momo.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenProviderTest {

    private TokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new TokenProvider(
                "TW9tb1Rlc3RPbmx5U2VjcmV0S2V5Rm9ySlZUVGVzdHMxMjM0NQ==",
                1_800_000L,
                604_800_000L);
    }

    @Test
    void accessTokenIsAcceptedOnlyAsAccessToken() {
        String accessToken = tokenProvider.createToken("momo-user");

        assertTrue(tokenProvider.validateToken(accessToken));
        assertFalse(tokenProvider.validateRefreshToken(accessToken));
    }

    @Test
    void refreshTokenIsAcceptedOnlyAsRefreshToken() {
        String refreshToken = tokenProvider.createRefreshToken("momo-user");

        assertTrue(tokenProvider.validateRefreshToken(refreshToken));
        assertFalse(tokenProvider.validateToken(refreshToken));
    }

    @Test
    void expiredAndMalformedRefreshTokensAreRejected() {
        TokenProvider expiredTokenProvider = new TokenProvider(
                "TW9tb1Rlc3RPbmx5U2VjcmV0S2V5Rm9ySlZUVGVzdHMxMjM0NQ==",
                1_800_000L,
                -1L);

        assertFalse(expiredTokenProvider.validateRefreshToken(
                expiredTokenProvider.createRefreshToken("momo-user")));
        assertFalse(tokenProvider.validateRefreshToken("not-a-jwt"));
    }
}
