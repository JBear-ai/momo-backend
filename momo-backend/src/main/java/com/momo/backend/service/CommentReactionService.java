
package com.momo.backend.service;

import com.momo.backend.entity.Comment;
import com.momo.backend.entity.CommentReaction;
import com.momo.backend.entity.User;
import com.momo.backend.exception.CustomException;
import com.momo.backend.repository.CommentReactionRepository;
import com.momo.backend.repository.CommentRepository;
import com.momo.backend.repository.ReviewRepository;
import com.momo.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CommentReactionService {

    // 댓글 반응, 댓글, 회원 DB 접근
    private final CommentReactionRepository reactionRepository;
    private final CommentRepository commentRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    // 로그인한 사용자의 댓글 좋아요/싫어요 저장 및 변경
    public void saveReaction(String loginId, Long commentId, Boolean liked) {

        // 로그인한 회원 조회
        User user = userRepository.findByLoginIdForUpdate(loginId)
                .orElseThrow(() ->
                        new CustomException(
                                "USER_NOT_FOUND",
                                "회원 정보를 찾을 수 없습니다."));

        // 부모 리뷰를 먼저 잠가 리뷰 삭제와 댓글 반응 저장의 잠금 순서를 맞춘다.
        Comment commentReference = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(
                        "COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다."));
        Long reviewId = commentReference.getReview().getReviewId();

        reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(() -> new CustomException(
                        "REVIEW_NOT_FOUND", "리뷰를 찾을 수 없습니다."));

        // 부모 리뷰 잠금 이후 댓글 행을 잠근다.
        Comment comment = commentRepository.findByIdForUpdate(commentId)
                .orElseThrow(() -> new CustomException(
                        "COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다."));

        // 좋아요/싫어요 선택 여부 확인
        if (liked == null) {
            throw new IllegalArgumentException("좋아요 또는 싫어요를 선택해주세요.");
        }

        // 회원 행 잠금이 같은 회원의 동시 반응 요청을 직렬화하므로
        // 조회 후 INSERT/UPDATE를 수행해 DB 고유 제약조건 충돌을 방지한다.
        CommentReaction reaction = reactionRepository
                .findByUser_UserIdAndComment_CommentId(user.getUserId(), commentId)
                .orElse(null);

        if (reaction == null) {
            reactionRepository.save(new CommentReaction(user, comment, liked));
        } else {
            reaction.updateReaction(liked);
        }
    }

    // 로그인한 사용자의 댓글 반응 취소
    public void deleteReaction(String loginId, Long commentId) {

        // 로그인한 회원 조회
        User user = userRepository.findByLoginIdForUpdate(loginId)
                .orElseThrow(() ->
                        new CustomException(
                                "USER_NOT_FOUND",
                                "회원 정보를 찾을 수 없습니다."));

        // 리뷰 삭제와 댓글 삭제에 같은 잠금 순서를 사용한다.
        Comment commentReference = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(
                        "COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다."));
        Long reviewId = commentReference.getReview().getReviewId();

        reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(() -> new CustomException(
                        "REVIEW_NOT_FOUND", "리뷰를 찾을 수 없습니다."));

        commentRepository.findByIdForUpdate(commentId)
                .orElseThrow(() -> new CustomException(
                        "COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다."));

        // 본인이 남긴 반응 조회
        CommentReaction reaction = reactionRepository
                .findByUser_UserIdAndComment_CommentId(
                        user.getUserId(), commentId
                )
                .orElseThrow(() ->
                        new CustomException(
                                "REACTION_NOT_FOUND",
                                "댓글 반응을 찾을 수 없습니다."));

        // 기존 반응 삭제
        reactionRepository.delete(reaction);
    }

    // 특정 댓글의 좋아요/싫어요 개수 조회
    @Transactional(readOnly = true)
    public long[] getReactionCounts(Long commentId) {

        // 댓글 존재 여부와 좋아요·싫어요 수를 단일 쿼리로 조회한다.
        CommentReactionRepository.CommentExistsAndReactionCountSummary summary =
                reactionRepository.findResponseSummaryByCommentId(commentId);
        if (summary == null) {
            throw new CustomException(
                    "COMMENT_NOT_FOUND",
                    "댓글을 찾을 수 없습니다.");
        }

        long likeCount = summary.getLikeCount();
        long dislikeCount = summary.getDislikeCount();

        return new long[]{likeCount, dislikeCount};
    }
}
