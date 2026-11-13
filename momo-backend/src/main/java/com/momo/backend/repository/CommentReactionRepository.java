
package com.momo.backend.repository;

import com.momo.backend.entity.CommentReaction;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentReactionRepository
        extends JpaRepository<CommentReaction, Long> {

    interface CommentReactionCountSummary {
        Long getCommentId();
        Long getLikeCount();
        Long getDislikeCount();
    }

    interface CommentUserReactionSummary {
        Long getCommentId();
        Boolean getLiked();
    }

    interface ReactionCountSummary {
        long getLikeCount();
        long getDislikeCount();
    }

    interface CommentResponseSummary {
        Long getLikeCount();
        Long getDislikeCount();
        Integer getMyReaction();
    }

    // 댓글 생성·수정 응답의 반응 수와 현재 회원의 반응을 한 번에 집계한다.
    @Query("""
            select count(case when reaction.liked = true then 1 end) as likeCount,
                   count(case when reaction.liked = false then 1 end) as dislikeCount,
                   max(case when reaction.user.userId = :userId
                            then case when reaction.liked = true then 1 else 0 end
                            else null end) as myReaction
            from CommentReaction reaction
            where reaction.comment.commentId = :commentId
            """)
    CommentResponseSummary findResponseSummaryByCommentId(
            @Param("commentId") Long commentId,
            @Param("userId") Long userId
    );

    // 단일 댓글의 좋아요/싫어요 수를 한 번의 DB 집계 쿼리로 조회한다.
    @Query("""
            select count(case when reaction.liked = true then 1 end) as likeCount,
                   count(case when reaction.liked = false then 1 end) as dislikeCount
            from CommentReaction reaction
            where reaction.comment.commentId = :commentId
            """)
    ReactionCountSummary findCountsByCommentId(@Param("commentId") Long commentId);

    // 댓글별 좋아요/싫어요 개수를 DB에서 집계해 한 번에 조회한다.
    @Query("""
            select reaction.comment.commentId as commentId,
                   sum(case when reaction.liked = true then 1 else 0 end) as likeCount,
                   sum(case when reaction.liked = false then 1 else 0 end) as dislikeCount
            from CommentReaction reaction
            where reaction.comment.commentId in :commentIds
            group by reaction.comment.commentId
            """)
    List<CommentReactionCountSummary> findReactionCountsByCommentIds(
            @Param("commentIds") Collection<Long> commentIds
    );

    // 로그인 사용자의 댓글별 반응도 한 번에 조회한다.
    @Query("""
            select reaction.comment.commentId as commentId,
                   reaction.liked as liked
            from CommentReaction reaction
            where reaction.comment.commentId in :commentIds
              and reaction.user.userId = :userId
            """)
    List<CommentUserReactionSummary> findMyReactionsByCommentIds(
            @Param("commentIds") Collection<Long> commentIds,
            @Param("userId") Long userId
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

    // 회원 탈퇴 시 본인이 작성한 댓글 반응을 한 번의 DELETE 쿼리로 정리한다.
    @Modifying
    @Query("delete from CommentReaction reaction where reaction.user.userId = :userId")
    int deleteAuthoredByUserId(@Param("userId") Long userId);

    // 특정 회원이 작성한 댓글에 달린 모든 반응 삭제
    void deleteByComment_User_UserId(Long userId);

    // 회원 탈퇴 시 작성자 기준으로 연결된 댓글 반응을 한 번에 정리한다.
    @Modifying
    @Query("delete from CommentReaction reaction where reaction.comment.review.user.userId = :userId")
    int deleteForReviewsAuthoredByUser(@Param("userId") Long userId);

    @Modifying
    @Query("delete from CommentReaction reaction where reaction.comment.user.userId = :userId")
    int deleteForCommentsAuthoredByUser(@Param("userId") Long userId);

    // 특정 회원이 누른 댓글 좋아요 개수 조회
    long countByUser_UserIdAndLiked(Long userId, Boolean liked);
}
