package com.momo.backend.service;

import com.momo.backend.client.KobisApiClient;
import com.momo.backend.dto.movie.MovieCatalogSyncStatusResponse;
import com.momo.backend.dto.movie.MovieSearchResponse;
import com.momo.backend.entity.MovieCatalogSyncState;
import com.momo.backend.repository.MovieCatalogSyncStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieCatalogSyncService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final int PAGE_SIZE = 100;

    private final KobisApiClient kobisApiClient;
    private final ObjectMapper objectMapper;
    private final MoviePersistenceService moviePersistenceService;
    private final MovieService movieService;
    private final MovieCatalogSyncStateRepository stateRepository;

    @Value("${momo.catalog.sync.enabled:true}")
    private boolean enabled;

    public synchronized void syncNextPage() {
        if (!enabled) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);
        MovieCatalogSyncState state = stateRepository.findById(1)
                .orElseGet(() -> stateRepository.save(MovieCatalogSyncState.initial()));

        if (!state.canRetry(now)) {
            return;
        }

        if (state.needsRecentSync(now)) {
            int currentYear = LocalDate.now(KOREA_ZONE).getYear();
            state.startRecentSync(currentYear - 1, currentYear, now);
            stateRepository.save(state);
        }

        if (!state.isActive()) {
            return;
        }

        try {
            String response = "INITIAL".equals(state.getPhase())
                    ? kobisApiClient.searchCatalogMovies(state.getNextPage(), PAGE_SIZE, null, null)
                    : kobisApiClient.searchCatalogMovies(
                            state.getNextPage(), PAGE_SIZE,
                            state.getStartYear(), state.getEndYear());

            JsonNode root = objectMapper.readTree(response);
            JsonNode result = root.path("movieListResult");
            JsonNode list = result.path("movieList");
            JsonNode totalNode = result.path("totCnt");

            if (!list.isArray() || !totalNode.canConvertToLong() || totalNode.asLong() < 0) {
                throw new IllegalStateException("KOBIS 영화 카탈로그 응답 형식이 올바르지 않습니다.");
            }

            int reportedPages = (int) Math.ceil((double) totalNode.asLong() / PAGE_SIZE);
            if (list.isEmpty() && state.getNextPage() <= reportedPages) {
                throw new IllegalStateException("KOBIS 응답의 총건수와 현재 페이지 데이터가 일치하지 않습니다.");
            }

            List<MovieSearchResponse> movies = parseMovies(list);
            if (!list.isEmpty() && movies.isEmpty()) {
                throw new IllegalStateException("KOBIS 페이지에 유효한 영화 항목이 없습니다.");
            }
            if (!movies.isEmpty()) {
                moviePersistenceService.saveSearchMovies(movies);
                movieService.enqueueCatalogPosterSearches(movies);
            }

            int page = state.getNextPage();
            state.completePage(page, PAGE_SIZE, totalNode.asLong(), now);
            stateRepository.save(state);

            log.info("영화 카탈로그 수집 진행. phase={}, page={}/{}, pageCount={}, totalCount={}",
                    state.getPhase(), page, state.getTotalPages(), movies.size(), state.getTotalCount());
        } catch (Exception exception) {
            state.recordFailure(exception.getMessage(), now);
            stateRepository.save(state);
            log.error("영화 카탈로그 수집 실패. phase={}, page={}. 1분 후 재시도합니다.",
                    state.getPhase(), state.getNextPage(), exception);
        }
    }

    public MovieCatalogSyncStatusResponse getStatus() {
        MovieCatalogSyncState state = stateRepository.findById(1)
                .orElseGet(() -> MovieCatalogSyncState.initial());

        return new MovieCatalogSyncStatusResponse(
                state.getPhase(),
                state.getNextPage(),
                state.getTotalPages(),
                state.getTotalCount(),
                state.getStartYear(),
                state.getEndYear(),
                state.getLastCompletedAt(),
                state.getLastError()
        );
    }

    private List<MovieSearchResponse> parseMovies(JsonNode movieList) {
        List<MovieSearchResponse> movies = new ArrayList<>();
        for (JsonNode movie : movieList) {
            String movieCode = movie.path("movieCd").asString("");
            String title = movie.path("movieNm").asString("");
            if (!movieCode.matches("\\d{8}") || title.isBlank()) {
                continue;
            }

            String director = StreamSupport.stream(
                            movie.path("directors").spliterator(), false)
                    .map(node -> node.path("peopleNm").asString(""))
                    .filter(name -> !name.isBlank())
                    .distinct()
                    .limit(5)
                    .reduce((first, second) -> first + ", " + second)
                    .orElse("");

            movies.add(new MovieSearchResponse(
                    movieCode,
                    title,
                    movie.path("openDt").asString(""),
                    movie.path("genreAlt").asString(""),
                    director
            ));
        }
        return movies;
    }
}
