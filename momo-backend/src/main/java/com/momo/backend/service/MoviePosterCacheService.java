
package com.momo.backend.service;

import com.momo.backend.entity.MoviePosterCache;
import com.momo.backend.repository.MoviePosterCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MoviePosterCacheService {

    private final MoviePosterCacheRepository posterCacheRepository;

    // 영화 포스터 캐시 조회
    public Optional<MoviePosterCache> findCache(String movieCode) {
        return posterCacheRepository.findById(movieCode);
    }

    // 포스터 캐시 생성
    @Transactional
    public MoviePosterCache createCache(
            String movieCode,
            String title,
            String releaseDate
    ) {
        return posterCacheRepository.findById(movieCode)
                .orElseGet(() -> posterCacheRepository.save(
                        new MoviePosterCache(
                                movieCode,
                                title,
                                releaseDate
                        )
                ));
    }

    // 포스터 조회 진행 상태 변경
    @Transactional
    public void markAsProcessing(String movieCode) {
        posterCacheRepository.findById(movieCode)
                .ifPresent(MoviePosterCache::markAsProcessing);
    }

    // 포스터 조회 성공 및 주소 저장
    @Transactional
    public void updatePoster(String movieCode, String posterUrl) {
        posterCacheRepository.findById(movieCode)
                .ifPresent(cache -> cache.updatePoster(posterUrl));
    }

    // 포스터 조회 실패 처리
    @Transactional
    public void markAsFailed(String movieCode) {
        posterCacheRepository.findById(movieCode)
                .ifPresent(MoviePosterCache::markAsFailed);
    }
}
