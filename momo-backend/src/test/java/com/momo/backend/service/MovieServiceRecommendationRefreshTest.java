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
        return spy(new MovieService(
                mock(MoviePosterAsyncService.class),
                mock(KobisApiClient.class),
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

        movie.markWeeklyBoxOfficeEntered(4);

        assertEquals("20261112", movie.getLastBoxOfficeDate());
        assertEquals(1, movie.getLastBoxOfficeRank());
        assertEquals(1, movie.getBestBoxOfficeRank());
    }

    @Test
    void normalizesWeeklyKobisReleaseDateBeforePersistence() {
        assertEquals("20200102", MovieService.normalizeKobisReleaseDate("2020-01-02"));
        assertEquals("20200102", MovieService.normalizeKobisReleaseDate("20200102"));
        assertEquals("", MovieService.normalizeKobisReleaseDate(""));
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
        doReturn(List.of()).when(service).getDailyBoxOffice(anyString());
        assertTrue(service.getBoxOfficeRecommendations(null).isEmpty());
        assertTrue(service.getBoxOfficeRecommendations(null).isEmpty());

        // 추천 API는 일별 이력만 확인하고, 주간 16주 수집은 별도 스케줄러가 담당한다.
        verify(service, times(7)).getDailyBoxOffice(anyString());
        verify(service, never()).getWeeklyBoxOffice(anyString());
    }
}
