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
                mock(Cache.class)
        ));
        doReturn(List.of()).when(service).getDailyBoxOffice(anyString());

        assertTrue(service.getBoxOfficeRecommendations(null).isEmpty());
        assertTrue(service.getBoxOfficeRecommendations(null).isEmpty());

        // 첫 호출만 어제부터 7일치 이력을 확인하고, 두 번째 추천 조회는 반복 작업을 생략한다.
        verify(service, times(7)).getDailyBoxOffice(anyString());
    }
}
