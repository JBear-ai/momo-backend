
package com.momo.backend.service;

import com.momo.backend.dto.review.ReviewResponse;
import com.momo.backend.entity.Review;
import com.momo.backend.entity.ReviewReactions;
import com.momo.backend.entity.User;
import com.momo.backend.exception.CustomException;
import com.momo.backend.repository.ReviewReactionRepository;
import com.momo.backend.repository.ReviewRepository;
import com.momo.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
        User user = userRepository.findByLoginIdForUpdate(loginId)
                .orElseThrow(() ->
                        new CustomException(
                                "USER_NOT_FOUND",
                                "회원 정보를 찾을 수 없습니다."));

        // 반응을 남길 리뷰 조회
        Review review = reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(() ->
                        new CustomException(
                                "REVIEW_NOT_FOUND",
                                "리뷰를 찾을 수 없습니다."));

        // 좋아요/싫어요 선택 여부 확인
        if (liked == null) {
            throw new IllegalArgumentException("좋아요 또는 싫어요를 선택해주세요.");
        }

        // 회원 행 잠금이 같은 회원의 동시 반응 요청을 직렬화하므로
        // 조회 후 INSERT/UPDATE를 수행해 DB 고유 제약조건 충돌을 방지한다.
        ReviewReactions reactions = reactionRepository
                .findByUser_UserIdAndReview_ReviewId(user.getUserId(), reviewId)
                .orElse(null);

        if (reactions == null) {
            reactionRepository.save(new ReviewReactions(user, review, liked));
        } else {
            reactions.updateReaction(liked);
        }
    }

    // 로그인한 사용자의 리뷰 반응 취소
    public void deleteReaction(String loginId, Long reviewId) {

        // 로그인한 회원 조회
        User user = userRepository.findByLoginIdForUpdate(loginId)
                .orElseThrow(() ->
                        new CustomException(
                                "USER_NOT_FOUND",
                                "회원 정보를 찾을 수 없습니다."));

        // 리뷰 삭제와 반응 삭제를 같은 리뷰 행 잠금으로 직렬화한다.
        reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(() -> new CustomException(
                        "REVIEW_NOT_FOUND", "리뷰를 찾을 수 없습니다."));

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

    // 로그인 회원이 특정 리뷰에 남긴 반응 조회 (없으면 null)
    @Transactional(readOnly = true)
    public Boolean getMyReaction(String loginId, Long reviewId) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new CustomException(
                                "USER_NOT_FOUND",
                                "회원 정보를 찾을 수 없습니다."));

        if (!reviewRepository.existsById(reviewId)) {
            throw new CustomException(
                    "REVIEW_NOT_FOUND",
                    "리뷰를 찾을 수 없습니다.");
        }

        return reactionRepository
                .findByUser_UserIdAndReview_ReviewId(user.getUserId(), reviewId)
                .map(reaction -> reaction == null ? null : reaction.getLiked())
                .orElse(null);
    }

    // 특정 리뷰의 좋아요/싫어요 개수 조회
    @Transactional(readOnly = true)
    public long[] getReactionCounts(Long reviewId) {

        // 리뷰 존재 여부와 좋아요·싫어요 수를 단일 쿼리로 조회한다.
        ReviewReactionRepository.ReviewResponseSummary summary =
                reactionRepository.findResponseSummaryByReviewId(reviewId);
        if (summary == null) {
            throw new CustomException(
                    "REVIEW_NOT_FOUND",
                    "리뷰를 찾을 수 없습니다.");
        }

        long likeCount = summary.getLikeCount();
        long dislikeCount = summary.getDislikeCount();

        return new long[]{likeCount, dislikeCount};
    }

    // 로그인한 회원이 좋아요한 리뷰 목록 조회
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyLikedReviews(
            String loginId,
            int page
    ) {

        // 로그인한 회원 조회
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new CustomException(
                                "USER_NOT_FOUND",
                                "회원 정보를 찾을 수 없습니다."
                        ));

        // 페이지 설정
        Pageable pageable = PageRequest.of(page, 10);

        // 좋아요한 리뷰 조회
        return reactionRepository
                .findByUser_UserIdAndLikedOrderByReview_CreatedAtDescReview_ReviewIdDesc(
                        user.getUserId(),
                        true,
                        pageable
                )
                .map(reaction ->
                        new ReviewResponse(reaction.getReview())
                );
    }

}
