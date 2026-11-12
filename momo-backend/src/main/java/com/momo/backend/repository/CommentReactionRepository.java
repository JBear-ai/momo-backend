
package com.momo.backend.repository;

import com.momo.backend.entity.CommentReaction;\nimport org.springframework.data.jpa.repository.Query;\nimport org.springframework.data.repository.query.Param;\n\nimport java.util.Collection;\nimport java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentReactionRepository
        extends JpaRepository<CommentReaction, Long> {

    interface CommentReactionSummary {
        Long getCommentId();
        Boolean getLiked();
        Long getUserId();
    }

    // 댓글 페이지에 필요한 반응 정보만 한 번에 조회한다.
    @Query("""
            select reaction.comment.commentId as commentId,
                   reaction.liked as liked,
                   reaction.user.userId as userId
            from CommentReaction reaction
            where reaction.comment.commentId in :commentIds
            """)
    List<CommentReactionSummary> findReactionSummariesByCommentIds(
            @Param("commentIds") Collection<Long> commentIds
    );

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

    // 특정 회원이 누른 댓글 좋아요 개수 조회
    long countByUser_UserIdAndLiked(Long userId, Boolean liked);
}
