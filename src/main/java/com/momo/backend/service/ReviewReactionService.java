
package com.momo.backend.service;

import com.momo.backend.entity.Review;
import com.momo.backend.entity.ReviewReactions;
import com.momo.backend.entity.User;
import com.momo.backend.exception.CustomException;
import com.momo.backend.repository.ReviewReactionRepository;
import com.momo.backend.repository.ReviewRepository;
import com.momo.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ReviewReactionService {

    // 리뷰 반응, 리뷰, 회원 DB 접근
    private final ReviewReactionRepository reactionRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    // 로그인한 사용자의 리뷰 좋아요/싫어요 저장 및 변경
    public void saveReactions(String loginId, Long reviewId, Boolean liked) {

        // 로그인한 회원 조회
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new CustomException(
                                "USER_NOT_FOUND",
                                "회원 정보를 찾을 수 없습니다."));

        // 반응을 남길 리뷰 조회
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new CustomException(
                                "REVIEW_NOT_FOUND",
                                "리뷰를 찾을 수 없습니다."));

        // 좋아요/싫어요 선택 여부 확인
        if (liked == null) {
            throw new IllegalArgumentException("좋아요 또는 싫어요를 선택해주세요.");
        }

        // 기존에 남긴 반응이 있는지 확인
        ReviewReactions reactions = reactionRepository
                .findByUser_UserIdAndReview_ReviewId(
                        user.getUserId(), reviewId
                )
                .orElse(null);

        if (reactions == null) {

            // 반응이 없으면 새로 저장
            reactionRepository.save(
                    new ReviewReactions(user, review, liked)
            );

        } else {

            // 기존 반응이 있으면 좋아요/싫어요 변경
            reactions.updateReaction(liked);
        }
    }

    // 로그인한 사용자의 리뷰 반응 취소
    public void deleteReaction(String loginId, Long reviewId) {

        // 로그인한 회원 조회
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new CustomException(
                                "USER_NOT_FOUND",
                                "회원 정보를 찾을 수 없습니다."));

        // 본인이 남긴 반응 조회
        ReviewReactions reactions = reactionRepository
                .findByUser_UserIdAndReview_ReviewId(
                        user.getUserId(), reviewId
                )
                .orElseThrow(() ->
                        new CustomException(
                                "REACTION_NOT_FOUND",
                                "리뷰 반응을 찾을 수 없습니다."));

        // 기존 반응 삭제
        reactionRepository.delete(reactions);
    }

    // 특정 리뷰의 좋아요/싫어요 개수 조회
    @Transactional(readOnly = true)
    public long[] getReactionCounts(Long reviewId) {

        // 리뷰가 존재하는지 확인
        if (!reviewRepository.existsById(reviewId)) {
            throw new CustomException(
                    "REVIEW_NOT_FOUND",
                    "리뷰를 찾을 수 없습니다.");
        }

        // 좋아요 개수 조회
        long likeCount = reactionRepository
                .countByReview_ReviewIdAndLiked(reviewId, true);

        // 싫어요 개수 조회
        long dislikeCount = reactionRepository
                .countByReview_ReviewIdAndLiked(reviewId, false);

        return new long[]{likeCount, dislikeCount};
    }

}
