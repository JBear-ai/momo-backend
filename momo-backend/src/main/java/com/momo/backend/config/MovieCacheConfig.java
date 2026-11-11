package com.momo.backend.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.momo.backend.entity.MovieCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class MovieCacheConfig {

    /**
     * L1 영화 API 메모리 캐시.
     * MySQL의 기존 movie_cache가 L2 역할을 하며,
     * 메모리 캐시는 최대 15분 동안만 데이터를 보유한다.
     */
    @Bean
    public Cache<String, MovieCache> movieApiMemoryCache() {
        return Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(Duration.ofMinutes(15))
                .recordStats()
                .build();
    }
}
