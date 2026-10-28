
package com.momo.backend.service;

import com.momo.backend.entity.Comment;
import com.momo.backend.entity.CommentReaction;
import com.momo.backend.entity.User;
import com.momo.backend.exception.CustomException;
import com.momo.backend.repository.CommentReactionRepository;
import com.momo.backend.repository.CommentRepository;
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
    private final UserRepository userRepository;

    // 로그인한 사용자의 댓글 좋아요/싫어요 저장 및 변경
    public void saveReaction(String loginId, Long commentId, Boolean liked) {

        // 로그인한 회원 조회
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new CustomException(
                                "USER_NOT_FOUND",
                                "회원 정보를 찾을 수 없습니다."));

        // 반응을 남길 댓글 조회
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() ->
                        new CustomException(
                                "COMMENT_NOT_FOUND",
                                "댓글을 찾을 수 없습니다."));

        // 좋아요/싫어요 선택 여부 확인
        if (liked == null) {
            throw new IllegalArgumentException("좋아요 또는 싫어요를 선택해주세요.");
        }

        // 기존에 남긴 반응이 있는지 확인
        CommentReaction reaction = reactionRepository
                .findByUser_UserIdAndComment_CommentId(
                        user.getUserId(), commentId
                )
                .orElse(null);

        if (reaction == null) {

            // 반응이 없으면 새로 저장
            reactionRepository.save(
                    new CommentReaction(user, comment, liked)
            );

        } else {

            // 기존 반응이 있으면 좋아요/싫어요 변경
            reaction.updateReaction(liked);
        }
    }

    // 로그인한 사용자의 댓글 반응 취소
    public void deleteReaction(String loginId, Long commentId) {

        // 로그인한 회원 조회
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new CustomException(
                                "USER_NOT_FOUND",
                                "회원 정보를 찾을 수 없습니다."));

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

        // 댓글이 존재하는지 확인
        if (!commentRepository.existsById(commentId)) {
            throw new CustomException(
                    "COMMENT_NOT_FOUND",
                    "댓글을 찾을 수 없습니다.");
        }

        // 좋아요 개수 조회
        long likeCount = reactionRepository
                .countByComment_CommentIdAndLiked(commentId, true);

        // 싫어요 개수 조회
        long dislikeCount = reactionRepository
                .countByComment_CommentIdAndLiked(commentId, false);

        return new long[]{likeCount, dislikeCount};
    }
}
