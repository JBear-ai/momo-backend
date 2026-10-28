package com.momo.backend.repository;

import com.momo.backend.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 특정 리뷰의 댓글을 오래된 순서로 조회 (페이지 처리)
    Page<Comment> findByReview_ReviewIdOrderByCreatedAtAsc(
            Long reviewId, Pageable pageable
    );

    // 특정 리뷰에 작성된 댓글 개수 확인
    long countByReview_ReviewId(Long reviewId);

    // 리뷰 삭제 시 해당 리뷰의 댓글을 먼저 삭제
    void deleteByReview_ReviewId(Long reviewId);

    // 특정 회원이 작성한 댓글 전체 삭제
    void deleteByUser_UserId(Long userId);
}