package com.momo.backend.repository;

import com.momo.backend.entity.ReviewReactions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewReactionRepository
        extends JpaRepository<ReviewReactions, Long> {

    interface ReactionCountSummary {
        long getLikeCount();
        long getDislikeCount();
    }

    interface ReviewReactionCountSummary {
        Long getReviewId();
        Long getLikeCount();
        Long getDislikeCount();
    }

    // 한 페이지 리뷰들의 좋아요·싫어요 수를 그룹 집계로 한 번에 조회한다.
    @Query("""
            select reaction.review.reviewId as reviewId,
                   sum(case when reaction.liked = true then 1 else 0 end) as likeCount,
                   sum(case when reaction.liked = false then 1 else 0 end) as dislikeCount
              from ReviewReactions reaction
             where reaction.review.reviewId in :reviewIds
             group by reaction.review.reviewId
            """)
    List<ReviewReactionCountSummary> findCountsByReviewIds(
            @Param("reviewIds") Collection<Long> reviewIds
    );

    // 리뷰의 좋아요/싫어요 수를 한 번의 DB 집계 쿼리로 조회한다.
    @Query("""
            select count(case when reaction.liked = true then 1 end) as likeCount,
                   count(case when reaction.liked = false then 1 end) as dislikeCount
            from ReviewReactions reaction
            where reaction.review.reviewId = :reviewId
            """)
    ReactionCountSummary findCountsByReviewId(@Param("reviewId") Long reviewId);

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

    // 회원 탈퇴 시 본인이 작성한 리뷰 반응을 한 번의 DELETE 쿼리로 정리한다.
    @Modifying
    @Query("delete from ReviewReactions reaction where reaction.user.userId = :userId")
    int deleteAuthoredByUserId(@Param("userId") Long userId);

    // 회원 탈퇴 시 본인이 작성한 리뷰에 달린 반응을 한 번에 삭제한다.
    @Modifying
    @Query("delete from ReviewReactions reaction where reaction.review.user.userId = :userId")
    int deleteForReviewsAuthoredByUser(@Param("userId") Long userId);

    // 특정 회원이 누른 리뷰 좋아요 개수 조회
    long countByUser_UserIdAndLiked(Long userId, Boolean liked);

    // 좋아요 목록의 리뷰·작성자 정보를 한 번에 조회해 페이지별 N+1을 방지한다.
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"review", "review.user"})
    Page<ReviewReactions>
    findByUser_UserIdAndLikedOrderByReview_CreatedAtDescReview_ReviewIdDesc(
            Long userId,
            Boolean liked,
            Pageable pageable
    );
}