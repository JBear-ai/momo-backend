
package com.momo.backend.repository;

import com.momo.backend.entity.MoviePosterCache;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

// 영화 포스터 캐시 조회 및 저장
public interface MoviePosterCacheRepository
        extends JpaRepository<MoviePosterCache, String> {

    // 여러 영화 코드에 해당하는 포스터 캐시를 한 번에 조회
    List<MoviePosterCache> findAllByMovieCodeIn(
            Collection<String> movieCodes
    );
}
