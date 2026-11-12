package com.momo.backend;

import com.momo.backend.entity.MoviePosterCache;
import com.momo.backend.repository.MoviePosterCacheRepository;
import com.momo.backend.security.TokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:momo;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "momo.storage.poster-dir=${java.io.tmpdir}/momo-poster-security-test",
        "JWT_SECRET=TW9tb1Rlc3RPbmx5U2VjcmV0S2V5Rm9ySlZUVGVzdHMxMjM0NQ==",
})
@ActiveProfiles("test")
class MomoBackendApplicationTests {

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MoviePosterCacheRepository posterCacheRepository;

    @Test
    void localPosterImageCanBeRequestedWithoutAnAuthorizationHeader() throws Exception {
        Path posterDirectory = Path.of(
                System.getProperty("java.io.tmpdir"),
                "momo-poster-security-test");
        Files.createDirectories(posterDirectory);

        String fileName = "anonymous-" + UUID.randomUUID() + ".jpg";
        Path posterFile = posterDirectory.resolve(fileName);
        Files.write(posterFile, new byte[] {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00
        });

        try {
            // 브라우저 img 요청처럼 Authorization 헤더 없이 실제 파일을 받아야 한다.
            mockMvc.perform(get("/posters/" + fileName))
                    .andExpect(status().isOk());
        } finally {
            Files.deleteIfExists(posterFile);
        }
    }

    @Test
    void contextLoads() {
    }

    @Test
    void signupLoginRefreshAndLogoutFlowKeepsTokensConsistent() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String loginId = "momo" + suffix;
        String nickname = "m" + suffix;

        String signupBody = """
                {
                  "loginId": "%s",
                  "password": "MomoTest1!",
                  "nickname": "%s",
                  "email": "%s@example.com"
                }
                """.formatted(loginId, nickname, loginId);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"%s","password":"MomoTest1!"}
                                """.formatted(loginId)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode login = objectMapper.readTree(
                loginResult.getResponse().getContentAsString());
        String accessToken = login.path("accessToken").asString("");
        String refreshToken = login.path("refreshToken").asString("");
        assertTrue(!accessToken.isBlank());
        assertTrue(!refreshToken.isBlank());

        String favoriteRequest = """
                {
                  "movieCode": "20261234",
                  "title": "관심 영화 테스트",
                  "releaseDate": "20261112",
                  "posterUrl": "/posters/20261234.jpg"
                }
                """;

        mockMvc.perform(post("/api/favorites")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(favoriteRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movieCode").value("20261234"));

        // 같은 영화 코드를 다시 등록해도 회원별 관심 영화는 중복되지 않는다.
        mockMvc.perform(post("/api/favorites")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(favoriteRequest))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("관심 영화 테스트"));

        // 다른 회원에게 첫 번째 회원의 관심 영화가 노출되지 않는지 확인한다.
        String secondLoginId = loginId + "b";
        String secondNickname = "n" + suffix;
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "%s",
                                  "password": "MomoTest1!",
                                  "nickname": "%s",
                                  "email": "%s@example.com"
                                }
                                """.formatted(secondLoginId, secondNickname, secondLoginId)))
                .andExpect(status().isCreated());

        MvcResult secondLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"%s","password":"MomoTest1!"}
                                """.formatted(secondLoginId)))
                .andExpect(status().isOk())
                .andReturn();
        String secondAccessToken = objectMapper.readTree(
                secondLoginResult.getResponse().getContentAsString())
                .path("accessToken").asString("");

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + secondAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        // 두 번째 계정 조회 이후에도 첫 번째 계정의 데이터는 그대로 유지되어야 한다.
        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(delete("/api/favorites/20261234")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode refreshed = objectMapper.readTree(
                refreshResult.getResponse().getContentAsString());
        String newAccessToken = refreshed.path("accessToken").asString("");
        String newRefreshToken = refreshed.path("refreshToken").asString("");
        assertTrue(!newAccessToken.isBlank());
        assertTrue(!newRefreshToken.isBlank());

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(newRefreshToken)))
                .andExpect(status().isUnauthorized());
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
