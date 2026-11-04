
package com.momo.backend.repository;

import com.momo.backend.entity.MovieRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MovieRatingRepository
        extends JpaRepository<MovieRating, Long> {

    // 특정 회원이 특정 영화에 남긴 평가 조회
    Optional<MovieRating> findByUser_UserIdAndMovie_MovieCode(
            Long userId,
            String movieCode
    );

    // 특정 영화의 좋아요 개수 조회
    long countByMovie_MovieCodeAndLiked(
            String movieCode,
            Boolean liked
    );

    // 특정 회원이 남긴 영화 평가 전체 삭제
    void deleteByUser_UserId(Long userId);
}
