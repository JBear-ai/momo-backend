
package com.momo.backend.repository;

import com.momo.backend.entity.MoviePosterCache;
import org.springframework.data.jpa.repository.JpaRepository;

// 영화 포스터 캐시 조회 및 저장
public interface MoviePosterCacheRepository
        extends JpaRepository<MoviePosterCache, String> {

}
