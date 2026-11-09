package com.momo.backend;

import com.momo.backend.entity.MoviePosterCache;
import com.momo.backend.repository.MoviePosterCacheRepository;
import com.momo.backend.security.TokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:momo;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "JWT_SECRET=TW9tb1Rlc3RPbmx5U2VjcmV0S2V5Rm9ySlZUVGVzdHMxMjM0NQ==",
})
@ActiveProfiles("test")
class MomoBackendApplicationTests {

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private MoviePosterCacheRepository posterCacheRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void createsAndValidatesJwtForLoginId() {
        String token = tokenProvider.createToken("momo-test-user");

        assertTrue(tokenProvider.validateToken(token));
        assertEquals("momo-test-user", tokenProvider.getAuthentication(token).getName());
    }

    @Test
    void stalePosterWorkerCannotOverwriteOrReleaseNewerClaim() {
        String movieCode = "20990001";
        posterCacheRepository.saveAndFlush(
                new MoviePosterCache(movieCode, "소유권 검증 영화", "20260101"));

        LocalDateTime firstClaimTime = LocalDateTime.now();
        String firstToken = UUID.randomUUID().toString();

        assertEquals(1, posterCacheRepository.tryClaim(
                movieCode,
                firstToken,
                firstClaimTime,
                firstClaimTime.minusHours(6),
                firstClaimTime.minusMinutes(10)));

        LocalDateTime takeoverTime = firstClaimTime.plusMinutes(11);
        String secondToken = UUID.randomUUID().toString();

        assertEquals(1, posterCacheRepository.tryClaim(
                movieCode,
                secondToken,
                takeoverTime,
                takeoverTime.minusHours(6),
                takeoverTime.minusMinutes(10)));

        assertEquals(0, posterCacheRepository.completeClaim(
                movieCode, firstToken, "https://example.com/old.jpg", takeoverTime));
        assertEquals(0, posterCacheRepository.failClaim(
                movieCode, firstToken, takeoverTime));
        assertEquals(0, posterCacheRepository.releaseClaim(
                movieCode, firstToken, takeoverTime));

        assertEquals(1, posterCacheRepository.completeClaim(
                movieCode, secondToken, "https://example.com/current.jpg", takeoverTime));

        MoviePosterCache saved = posterCacheRepository.findById(movieCode).orElseThrow();
        assertEquals("SUCCESS", saved.getStatus().name());
        assertEquals("https://example.com/current.jpg", saved.getPosterUrl());
        assertNull(saved.getClaimToken());
    }

    @Test
    void currentPosterWorkerCanMarkItsOwnClaimFailed() {
        String movieCode = "20990002";
        posterCacheRepository.saveAndFlush(
                new MoviePosterCache(movieCode, "실패 상태 검증 영화", "20260101"));

        LocalDateTime now = LocalDateTime.now();
        String claimToken = UUID.randomUUID().toString();

        assertEquals(1, posterCacheRepository.tryClaim(
                movieCode, claimToken, now, now.minusHours(6), now.minusMinutes(10)));
        assertEquals(1, posterCacheRepository.failClaim(movieCode, claimToken, now));

        MoviePosterCache saved = posterCacheRepository.findById(movieCode).orElseThrow();
        assertEquals("FAILED", saved.getStatus().name());
        assertNull(saved.getClaimToken());
    }
}
