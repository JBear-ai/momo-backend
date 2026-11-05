package com.momo.backend.repository;

import com.momo.backend.entity.MovieCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MovieCacheRepository
        extends JpaRepository<MovieCache, Long> {

    Optional<MovieCache> findByCacheTypeAndCacheKey(
            String cacheType,
            String cacheKey
    );
}