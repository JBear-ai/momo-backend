
package com.momo.backend.repository;

import com.momo.backend.entity.CommentReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentReactionRepository
        extends JpaRepository<CommentReaction, Long> {

    // 특정 회원이 특정 댓글에 남긴 반응 조회
    Optional<CommentReaction> findByUser_UserIdAndComment_CommentId(
            Long userId, Long commentId
    );

    // 특정 댓글의 좋아요/싫어요 개수 조회
    long countByComment_CommentIdAndLiked(
            Long commentId, Boolean liked
    );

    // 특정 댓글의 전체 반응 개수 조회
    long countByComment_CommentId(Long commentId);

    // 특정 댓글에 남겨진 모든 반응 삭제
    void deleteByComment_CommentId(Long commentId);

    // 특정 리뷰의 댓글에 남겨진 모든 반응 삭제
    void deleteByComment_Review_ReviewId(Long reviewId);

    // 특정 회원이 남긴 모든 댓글 반응 삭제
    void deleteByUser_UserId(Long userId);

    // 특정 회원이 작성한 댓글에 달린 모든 반응 삭제
    void deleteByComment_User_UserId(Long userId);
}
