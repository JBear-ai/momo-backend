package com.momo.backend.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.momo.backend.client.KobisApiClient;
import com.momo.backend.entity.Movie;
import com.momo.backend.repository.MovieCacheRepository;
import com.momo.backend.repository.MovieExternalIdMappingRepository;
import com.momo.backend.repository.MoviePosterCacheRepository;
import com.momo.backend.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MovieServiceRecommendationRefreshTest {

    @SuppressWarnings("unchecked")
    private Cache<String, com.momo.backend.entity.MovieCache> mockCache() {
        return (Cache<String, com.momo.backend.entity.MovieCache>) mock(Cache.class);
    }

    @Test
    void recommendsOnlyBoxOfficeMoviesReleasedAtLeastThreeMonthsAgoAndNotRankOne() {
        MovieRepository movieRepository = mock(MovieRepository.class);
        Movie eligible = new Movie(
                "20167904", "오래된 흥행작", "", "20260101",
                "", "드라마", "", "", "");
        eligible.markBoxOfficeEntered(2, "20261112");

        Movie tooRecent = new Movie(
                "20261112", "최근 개봉작", "", "20261112",
                "", "드라마", "", "", "");
        tooRecent.markBoxOfficeEntered(3, "20261112");

        Movie currentTopRank = new Movie(
                "20250654", "현재 1위", "", "20250601",
                "", "드라마", "", "", "");
        currentTopRank.markBoxOfficeEntered(1, "20261112");

        when(movieRepository.findByBoxOfficeEnteredTrueOrderByBestBoxOfficeRankAscLastBoxOfficeDateDesc())
                .thenReturn(List.of(eligible, tooRecent, currentTopRank));
        when(movieRepository.findById("20167904")).thenReturn(Optional.of(eligible));

        MovieService service = createService(movieRepository);
        doReturn(List.of()).when(service).getDailyBoxOffice(anyString());
        doReturn(List.of()).when(service).getWeeklyBoxOffice(anyString());

        List<java.util.Map<String, Object>> recommendations =
                service.getBoxOfficeRecommendations(null);

        assertEquals(1, recommendations.size());
        assertEquals("20167904", recommendations.get(0).get("movieCode"));
        assertEquals("오래된 흥행작", recommendations.get(0).get("title"));
    }

    private MovieService createService(MovieRepository movieRepository) {
        return createService(movieRepository, mock(KobisApiClient.class));
    }

    private MovieService createService(MovieRepository movieRepository, KobisApiClient kobisApiClient) {
        return spy(new MovieService(
                mock(MoviePosterAsyncService.class),
                kobisApiClient,
                mock(KmdbService.class),
                mock(TmdbService.class),
                mock(ObjectMapper.class),
                movieRepository,
                mock(MovieCacheRepository.class),
                mock(MoviePosterCacheService.class),
                mock(MoviePersistenceService.class),
                mock(MoviePosterCacheRepository.class),
                mock(MovieExternalIdMappingRepository.class),
                mock(PosterFileStorageService.class),
                mockCache()
        ));
    }

    @Test
    void weeklyHistoryDoesNotOverwriteLatestDailyRank() {
        Movie movie = new Movie(
                "20167904", "테스트 영화", "", "20260101",
                "", "드라마", "", "", "");
        movie.markBoxOfficeEntered(1, "20261112");

        movie.markWeeklyBoxOfficeEntered(4, "20261112");

        assertEquals("20261112", movie.getLastBoxOfficeDate());
        assertEquals(1, movie.getLastBoxOfficeRank());
        assertEquals(1, movie.getBestBoxOfficeRank());
    }

    @Test
    void newerWeeklyHistoryUpdatesLastRankWhenNoNewerDailyEntryExists() {
        Movie movie = new Movie(
                "20167904", "테스트 영화", "", "20260101",
                "", "드라마", "", "", "");
        movie.markBoxOfficeEntered(3, "20261110");

        movie.markWeeklyBoxOfficeEntered(2, "20261112");

        assertEquals("20261112", movie.getLastBoxOfficeDate());
        assertEquals(2, movie.getLastBoxOfficeRank());
    }

    @Test
    void normalizesWeeklyKobisReleaseDateBeforePersistence() {
        assertEquals("20200102", MovieService.normalizeKobisReleaseDate("2020-01-02"));
        assertEquals("20200102", MovieService.normalizeKobisReleaseDate("20200102"));
        assertEquals("", MovieService.normalizeKobisReleaseDate(""));
    }

    @Test
    void refreshesWeeklyHistoryInSmallBackgroundBatches() {
        MovieService service = createService(mock(MovieRepository.class));
        doReturn(List.of()).when(service).getWeeklyBoxOffice(anyString());

        service.refreshWeeklyBoxOfficeHistory();
        verify(service, times(4)).getWeeklyBoxOffice(anyString());

        service.refreshWeeklyBoxOfficeHistory();
        verify(service, times(8)).getWeeklyBoxOffice(anyString());
    }

    @Test
    void refreshesSevenDaysOnlyOnceDuringTheRefreshWindow() {
        MovieService service = spy(new MovieService(
                mock(MoviePosterAsyncService.class),
                mock(KobisApiClient.class),
                mock(KmdbService.class),
                mock(TmdbService.class),
                mock(ObjectMapper.class),
                mock(MovieRepository.class),
                mock(MovieCacheRepository.class),
                mock(MoviePosterCacheService.class),
                mock(MoviePersistenceService.class),
                mock(MoviePosterCacheRepository.class),
                mock(MovieExternalIdMappingRepository.class),
                mock(PosterFileStorageService.class),
                mockCache()
        ));
        doReturn(List.of()).when(service).getDailyBoxOffice(
                anyString(), eq(ApiCallBudgetService.TrafficType.PREFETCH));
        assertTrue(service.getBoxOfficeRecommendations(null).isEmpty());
        verify(service, never()).getDailyBoxOffice(anyString());
        verify(service, never()).getDailyBoxOffice(anyString(), any());

        service.refreshRecentBoxOfficeHistory();
        service.refreshRecentBoxOfficeHistory();

        // 백그라운드 갱신도 주기 안에서는 중복 수집하지 않고 사전 수집 예산을 사용한다.
        verify(service, times(7)).getDailyBoxOffice(
                anyString(), eq(ApiCallBudgetService.TrafficType.PREFETCH));
        verify(service, never()).getWeeklyBoxOffice(anyString());
    }

    @Test
    void returnsAvailableCandidatesAfterInitialBackgroundCollection() {
        MovieRepository repository = mock(MovieRepository.class);
        MovieService service = createService(repository);
        when(repository.findByBoxOfficeEnteredTrueOrderByBestBoxOfficeRankAscLastBoxOfficeDateDesc())
                .thenReturn(List.of());

        // 초기 DB가 비어 있어도 외부 수집을 기다리지 않고 빈 목록을 안전하게 반환한다.
        assertTrue(service.getBoxOfficeRecommendations(null).isEmpty());
        Movie collected = new Movie("20167904", "수집된 영화", "", "20200101",
                "", "드라마", "", "", "");
        collected.markBoxOfficeEntered(2, "20261112");
        when(repository.findByBoxOfficeEnteredTrueOrderByBestBoxOfficeRankAscLastBoxOfficeDateDesc())
                .thenReturn(List.of(collected));

        assertEquals(1, service.getBoxOfficeRecommendations(null).size());
        verify(service, never()).getDailyBoxOffice(anyString(), any());
        verify(service, never()).getWeeklyBoxOffice(anyString());
    }

    @Test
    void backgroundCollectionUsesPrefetchClientWhenTheCacheIsEmpty() {
        KobisApiClient client = mock(KobisApiClient.class);
        MovieService service = createService(mock(MovieRepository.class), client);
        when(client.getDailyBoxOffice("20261112", ApiCallBudgetService.TrafficType.PREFETCH))
                .thenThrow(new IllegalStateException("test failure"));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> service.getDailyBoxOffice("20261112", ApiCallBudgetService.TrafficType.PREFETCH));

        verify(client).getDailyBoxOffice("20261112", ApiCallBudgetService.TrafficType.PREFETCH);
        verify(client, never()).getDailyBoxOffice(anyString());
    }
}
