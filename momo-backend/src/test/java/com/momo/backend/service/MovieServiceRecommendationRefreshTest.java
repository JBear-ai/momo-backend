package com.momo.backend.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.momo.backend.client.KobisApiClient;
import com.momo.backend.repository.MovieCacheRepository;
import com.momo.backend.repository.MovieExternalIdMappingRepository;
import com.momo.backend.repository.MoviePosterCacheRepository;
import com.momo.backend.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MovieServiceRecommendationRefreshTest {

    @SuppressWarnings("unchecked")
    private Cache<String, com.momo.backend.entity.MovieCache> mockCache() {
        return (Cache<String, com.momo.backend.entity.MovieCache>) mock(Cache.class);
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
        doReturn(List.of()).when(service).getWeeklyBoxOffice(anyString());

        assertTrue(service.getBoxOfficeRecommendations(null).isEmpty());
        assertTrue(service.getBoxOfficeRecommendations(null).isEmpty());

        // 첫 호출에서 일별 7일 + 주간 16주를 확인하고, 두 번째 조회는 캐시 갱신 구간을 재사용한다.
        verify(service, times(7)).getDailyBoxOffice(anyString());
        verify(service, times(16)).getWeeklyBoxOffice(anyString());
    }
}
