package com.momo.backend.repository;

import com.momo.backend.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 댓글 작성자와 리뷰를 함께 조회해 응답 변환 시 추가 조회를 방지한다.
    @EntityGraph(attributePaths = {"user", "review"})
    // 특정 리뷰의 댓글을 오래된 순서로 조회 (페이지 처리)
    Page<Comment> findByReview_ReviewIdOrderByCreatedAtAsc(
            Long reviewId, Pageable pageable
    );

    // 특정 리뷰에 작성된 댓글 개수 확인
    long countByReview_ReviewId(Long reviewId);

    // 리뷰 삭제 시 해당 리뷰의 댓글을 먼저 삭제
    void deleteByReview_ReviewId(Long reviewId);

    // 회원 탈퇴 시 작성한 댓글을 한 번의 DELETE 쿼리로 정리한다.
    @Modifying
    @Query("delete from Comment comment where comment.user.userId = :userId")
    int deleteAuthoredByUserId(@Param("userId") Long userId);

    // 회원 탈퇴 시 본인이 작성한 리뷰에 달린 댓글을 한 번의 DELETE 쿼리로 정리한다.
    @Modifying
    @Query("delete from Comment comment where comment.review.user.userId = :userId")
    int deleteForReviewsAuthoredByUser(@Param("userId") Long userId);
}