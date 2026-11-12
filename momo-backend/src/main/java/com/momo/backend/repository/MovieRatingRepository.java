
package com.momo.backend.repository;

import com.momo.backend.entity.MovieRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MovieRatingRepository
        extends JpaRepository<MovieRating, Long> {

    interface MovieRatingCountSummary {
        long getLikeCount();
        long getDislikeCount();
    }

    // 좋아요와 싫어요를 한 번의 DB 집계 쿼리로 조회한다.
    @Query("""
            select count(case when rating.liked = true then 1 end) as likeCount,
                   count(case when rating.liked = false then 1 end) as dislikeCount
            from MovieRating rating
            where rating.movie.movieCode = :movieCode
            """)
    MovieRatingCountSummary findCountsByMovieCode(@Param("movieCode") String movieCode);

    // 특정 회원이 특정 영화에 남긴 평가 조회
    Optional<MovieRating> findByUser_UserIdAndMovie_MovieCode(
            Long userId,
            String movieCode
    );

    // 특정 회원이 좋아요한 영화 목록을 최근 평가 순서로 조회
    List<MovieRating> findByUser_UserIdAndLikedOrderByRatingIdDesc(
            Long userId,
            Boolean liked
    );

    // 특정 영화의 좋아요 개수 조회
    long countByMovie_MovieCodeAndLiked(
            String movieCode,
            Boolean liked
    );

    // 회원 탈퇴 시 본인의 영화 평가를 한 번의 DELETE 쿼리로 정리한다.
    @Modifying
    @Query("delete from MovieRating rating where rating.user.userId = :userId")
    int deleteAuthoredByUserId(@Param("userId") Long userId);
}
