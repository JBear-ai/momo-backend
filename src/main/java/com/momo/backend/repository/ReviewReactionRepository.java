package com.momo.backend.repository;

import com.momo.backend.entity.ReviewReactions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewReactionRepository
        extends JpaRepository<ReviewReactions, Long> {

    // 특정 회원이 특정 리뷰에 남긴 반응 조회
    Optional<ReviewReactions> findByUser_UserIdAndReview_ReviewId(
            Long userId, Long reviewId
    );

    // 특정 리뷰의 좋아요 개수 조회
    long countByReview_ReviewIdAndLiked(Long reviewId, Boolean liked);

    // 특정 리뷰의 전체 반응 개수 조회
    long countByReview_ReviewId(Long reviewId);

    // 특정 리뷰에 남겨진 모든 반응 삭제
    void deleteByReview_ReviewId(Long reviewId);

    // 특정 회원이 남긴 모든 리뷰 반응 삭제
    void deleteByUser_UserId(Long userId);
}