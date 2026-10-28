package com.momo.backend.repository;

import com.momo.backend.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 특정 영화의 리뷰를 최신순으로 조회 (페이지 처리)
    Page<Review> findByMovieCodeOrderByCreatedAtDesc(
            String movieCode, Pageable pageable
    );

    // 같은 회원이 같은 영화에 이미 리뷰를 작성했는지 확인
    boolean existsByUser_UserIdAndMovieCode(
            Long userId, String movieCode
    );

    // 특정 회원이 작성한 리뷰 조회
    Page<Review> findByUser_UserId(
            Long userId, Pageable pageable
    );

    // 특정 회원이 작성한 리뷰 전체 조회
    List<Review> findAllByUser_UserId(Long userId);

    // 특정 영화의 좋아요 또는 싫어요 개수 조회
    long countByMovieCodeAndLiked(String movieCode, Boolean liked);

}