
package com.momo.backend.repository;

import com.momo.backend.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 전체 영화의 리뷰를 최신순으로 조회
    Page<Review> findAllByOrderByCreatedAtDescReviewIdDesc(
            Pageable pageable
    );

    // 전체 리뷰 검색: 영화 제목, 감독, 배우, 닉네임, 리뷰 제목, 리뷰 내용
    @Query(
            value = """
                    SELECT r
                    FROM Review r
                    WHERE
                        :searchType = 'ALL'
                        AND (
                            LOWER(COALESCE(r.user.nickname, '')) LIKE :pattern
                            OR LOWER(COALESCE(r.title, '')) LIKE :pattern
                            OR LOWER(COALESCE(r.content, '')) LIKE :pattern
                            OR EXISTS (
                                SELECT m.movieCode
                                FROM Movie m
                                WHERE m.movieCode = r.movieCode
                                  AND (
                                      LOWER(COALESCE(m.title, '')) LIKE :pattern
                                      OR LOWER(COALESCE(m.director, '')) LIKE :pattern
                                      OR LOWER(COALESCE(m.actors, '')) LIKE :pattern
                                  )
                            )
                        )
                        OR (
                            :searchType = 'MOVIE'
                            AND EXISTS (
                                SELECT m.movieCode FROM Movie m
                                WHERE m.movieCode = r.movieCode
                                  AND LOWER(COALESCE(m.title, '')) LIKE :pattern
                            )
                        )
                        OR (
                            :searchType = 'DIRECTOR'
                            AND EXISTS (
                                SELECT m.movieCode FROM Movie m
                                WHERE m.movieCode = r.movieCode
                                  AND LOWER(COALESCE(m.director, '')) LIKE :pattern
                            )
                        )
                        OR (
                            :searchType = 'ACTOR'
                            AND EXISTS (
                                SELECT m.movieCode FROM Movie m
                                WHERE m.movieCode = r.movieCode
                                  AND LOWER(COALESCE(m.actors, '')) LIKE :pattern
                            )
                        )
                        OR (
                            :searchType = 'NICKNAME'
                            AND LOWER(COALESCE(r.user.nickname, '')) LIKE :pattern
                        )
                        OR (
                            :searchType = 'TITLE'
                            AND LOWER(COALESCE(r.title, '')) LIKE :pattern
                        )
                        OR (
                            :searchType = 'CONTENT'
                            AND LOWER(COALESCE(r.content, '')) LIKE :pattern
                        )
                    ORDER BY r.createdAt DESC, r.reviewId DESC
                    """,
            countQuery = """
                    SELECT COUNT(r)
                    FROM Review r
                    WHERE
                        (
                            :searchType = 'ALL'
                            AND (
                                LOWER(COALESCE(r.user.nickname, '')) LIKE :pattern
                                OR LOWER(COALESCE(r.title, '')) LIKE :pattern
                                OR LOWER(COALESCE(r.content, '')) LIKE :pattern
                                OR EXISTS (
                                    SELECT m.movieCode
                                    FROM Movie m
                                    WHERE m.movieCode = r.movieCode
                                      AND (
                                          LOWER(COALESCE(m.title, '')) LIKE :pattern
                                          OR LOWER(COALESCE(m.director, '')) LIKE :pattern
                                          OR LOWER(COALESCE(m.actors, '')) LIKE :pattern
                                      )
                                )
                            )
                        )
                        OR (
                            :searchType = 'MOVIE'
                            AND EXISTS (
                                SELECT m.movieCode FROM Movie m
                                WHERE m.movieCode = r.movieCode
                                  AND LOWER(COALESCE(m.title, '')) LIKE :pattern
                            )
                        )
                        OR (
                            :searchType = 'DIRECTOR'
                            AND EXISTS (
                                SELECT m.movieCode FROM Movie m
                                WHERE m.movieCode = r.movieCode
                                  AND LOWER(COALESCE(m.director, '')) LIKE :pattern
                            )
                        )
                        OR (
                            :searchType = 'ACTOR'
                            AND EXISTS (
                                SELECT m.movieCode FROM Movie m
                                WHERE m.movieCode = r.movieCode
                                  AND LOWER(COALESCE(m.actors, '')) LIKE :pattern
                            )
                        )
                        OR (
                            :searchType = 'NICKNAME'
                            AND LOWER(COALESCE(r.user.nickname, '')) LIKE :pattern
                        )
                        OR (
                            :searchType = 'TITLE'
                            AND LOWER(COALESCE(r.title, '')) LIKE :pattern
                        )
                        OR (
                            :searchType = 'CONTENT'
                            AND LOWER(COALESCE(r.content, '')) LIKE :pattern
                        )
                    """
    )
    Page<Review> searchAllReviews(
            @Param("searchType") String searchType,
            @Param("pattern") String pattern,
            Pageable pageable
    );

    // 특정 영화의 리뷰를 최신순으로 조회
    Page<Review> findByMovieCodeOrderByCreatedAtDesc(
            String movieCode,
            Pageable pageable
    );

    // 같은 회원이 같은 영화에 이미 리뷰를 작성했는지 확인
    boolean existsByUser_UserIdAndMovieCode(
            Long userId,
            String movieCode
    );

    // 특정 회원이 작성한 리뷰 조회
    Page<Review> findByUser_UserId(
            Long userId,
            Pageable pageable
    );

    // 회원 탈퇴 시 작성한 리뷰를 한 번의 DELETE 쿼리로 정리한다.
    @Modifying
    @Query("delete from Review review where review.user.userId = :userId")
    int deleteAuthoredByUserId(@Param("userId") Long userId);
}
