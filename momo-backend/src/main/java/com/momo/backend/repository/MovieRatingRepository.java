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

    interface MovieRatingResponseSummary {
        long getLikeCount();
        long getDislikeCount();
        Integer getMyRating();
    }

    // 전체 반응 수와 현재 회원의 반응을 한 번의 집계 쿼리로 조회한다.
    // loginId가 null이면 myRating은 null로 반환된다.
    @Query("""
            select count(case when rating.liked = true then 1 end) as likeCount,
                   count(case when rating.liked = false then 1 end) as dislikeCount,
                   max(case when rating.user.loginId = :loginId
                            then case when rating.liked = true then 1 else 0 end
                            else null end) as myRating
            from MovieRating rating
            where rating.movie.movieCode = :movieCode
            """)
    MovieRatingResponseSummary findResponseSummaryByMovieCode(
            @Param("movieCode") String movieCode,
            @Param("loginId") String loginId
    );

    // 기존 호출부와의 호환성을 위해 집계 전용 조회도 유지한다.
    @Query("""
            select count(case when rating.liked = true then 1 end) as likeCount,
                   count(case when rating.liked = false then 1 end) as dislikeCount
            from MovieRating rating
            where rating.movie.movieCode = :movieCode
            """)
    MovieRatingCountSummary findCountsByMovieCode(@Param("movieCode") String movieCode);

    Optional<MovieRating> findByUser_UserIdAndMovie_MovieCode(
            Long userId,
            String movieCode
    );

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"movie"})
    List<MovieRating> findByUser_UserIdAndLikedOrderByRatingIdDesc(
            Long userId,
            Boolean liked
    );

    long countByMovie_MovieCodeAndLiked(
            String movieCode,
            Boolean liked
    );

    @Modifying
    @Query("delete from MovieRating rating where rating.user.userId = :userId")
    int deleteAuthoredByUserId(@Param("userId") Long userId);
}
