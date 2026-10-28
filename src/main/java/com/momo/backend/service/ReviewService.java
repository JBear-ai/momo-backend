package com.momo.backend.service;

import com.momo.backend.dto.review.ReviewCreateRequest;
import com.momo.backend.dto.review.ReviewResponse;
import com.momo.backend.entity.Review;
import com.momo.backend.entity.User;
import com.momo.backend.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.momo.backend.dto.review.ReviewUpdateRequest;
import com.momo.backend.exception.CustomException;
import java.util.Map;
import java.util.HashMap;


@Service
@Transactional
@RequiredArgsConstructor
public class ReviewService {

    // 리뷰 및 회원 DB 접근
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    // 리뷰 삭제 시 해당 리뷰의 댓글도 함께 삭제
    private final CommentRepository commentRepository;

    // 리뷰 및 댓글 좋아요 삭제부분
    private final ReviewReactionRepository reviewReactionRepository;
    private final CommentReactionRepository commentReactionRepository;

    // 로그인한 사용자의 리뷰 작성
    public ReviewResponse createReview(
            String loginId,
            ReviewCreateRequest dto) {

        // 로그인 아이디로 작성자 조회
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new CustomException(
                                "USER_NOT_FOUND",
                                "회원 정보를 찾을 수 없습니다.")); // 예외처리 추가

        // 같은 영화에 이미 리뷰를 작성했는지 확인
        boolean exists = reviewRepository
                .existsByUser_UserIdAndMovieCode(
                        user.getUserId(),
                        dto.getMovieCode()
                );

        if (exists) {
            throw new CustomException(
                    "DUPLICATE_REVIEW",
                    "이미 해당 영화에 리뷰를 작성했습니다."); // 예외 처리 추가
        }

        // 요청 DTO의 데이터로 리뷰 Entity 생성
        Review review = new Review(
                user,
                dto.getMovieCode(),
                dto.getTitle(),
                dto.getContent(),
                dto.getLiked()
        );

        // 리뷰 저장 후 응답 DTO 반환
        Review savedReview = reviewRepository.save(review);

        return new ReviewResponse(savedReview);
    }

    // 리뷰 ID로 상세 정보 조회
    @Transactional(readOnly = true)
    public ReviewResponse getReview(Long reviewId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new CustomException(
                                "REVIEW_NOT_FOUND",
                                "리뷰를 찾을 수 없습니다."));

        return new ReviewResponse(review);
    }

    // 영화 코드에 해당하는 리뷰 목록을 최신순으로 조회
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMovieReviews(
            String movieCode, int page) {

        // 한 페이지에 리뷰 10개 표시
        Pageable pageable = PageRequest.of(
                page, 10
        );

        return reviewRepository
                .findByMovieCodeOrderByCreatedAtDesc(movieCode, pageable)
                .map(ReviewResponse::new);
    }


    // 로그인한 사용자가 작성한 리뷰 수정
    public ReviewResponse updateReview(
            Long reviewId,
            String loginId,
            ReviewUpdateRequest dto) {

        // 수정할 리뷰 조회
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new CustomException(
                                "REVIEW_NOT_FOUND",
                                "리뷰를 찾을 수 없습니다."));

        // 리뷰 작성자와 현재 로그인한 사용자 비교
        if (!review.getUser().getLoginId().equals(loginId)) {
            throw new CustomException(
                    "FORBIDDEN",
                    "본인이 작성한 리뷰만 수정할 수 있습니다.");
        }

        // 기존 Entity의 수정 메서드 재사용
        review.updateReview(
                dto.getTitle(),
                dto.getContent(),
                dto.getLiked()
        );

        // 트랜잭션 종료 시 JPA 변경 감지로 DB에 반영
        return new ReviewResponse(review);
    }


    // 로그인한 사용자가 작성한 리뷰 삭제
    public void deleteReview(Long reviewId, String loginId) {

        // 삭제할 리뷰 조회
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new CustomException(
                                "REVIEW_NOT_FOUND",
                                "리뷰를 찾을 수 없습니다."));

        // 리뷰 작성자와 현재 로그인한 사용자 비교
        if (!review.getUser().getLoginId().equals(loginId)) {
            throw new CustomException(
                    "FORBIDDEN",
                    "본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }

        // 리뷰에 작성된 댓글의 좋아요/싫어요 삭제
        commentReactionRepository.deleteByComment_Review_ReviewId(reviewId);

        // 리뷰에 작성된 댓글 삭제
        commentRepository.deleteByReview_ReviewId(reviewId);

        // 리뷰에 남겨진 좋아요/싫어요 삭제
        reviewReactionRepository.deleteByReview_ReviewId(reviewId);

        // 리뷰 삭제
        reviewRepository.delete(review);
    }


    // 특정 영화의 좋아요/싫어요 개수와 긍정 평가 비율 조회
    @Transactional(readOnly = true)
    public Map<String, Object> getMovieRating(String movieCode) {

        // 해당 영화의 좋아요 개수 조회
        long likeCount = reviewRepository
                .countByMovieCodeAndLiked(movieCode, true);

        // 해당 영화의 싫어요 개수 조회
        long dislikeCount = reviewRepository
                .countByMovieCodeAndLiked(movieCode, false);

        // 전체 평가 개수 계산
        long totalCount = likeCount + dislikeCount;

        // 긍정 평가 비율 계산 (평가가 없으면 null)
        Double positiveRate = null;

        // 전체 평가가 1개 이상일 때만 긍정 평가 비율 계산 (0으로 나누는 오류 방지)
        if (totalCount > 0) {

            // 좋아요 수를 전체 평가 수로 나눈 뒤 100을 곱해 백분율(%)로 변환
            // (double)은 정수 나눗셈으로 소수점이 사라지는 것을 방지
            positiveRate = (double) likeCount / totalCount * 100;

            // 소수점 첫째 자리까지 반올림
            // 10을 곱한 뒤 반올림하고 다시 10으로 나누어 첫째 자리까지 표시
            positiveRate = Math.round(positiveRate * 10) / 10.0;
        }

        // 계산한 결과를 JSON 응답에 사용할 데이터로 정리
        Map<String, Object> result = new HashMap<>();
        result.put("movieCode", movieCode);
        result.put("likeCount", likeCount);
        result.put("dislikeCount", dislikeCount);
        result.put("totalCount", totalCount);
        result.put("positiveRate", positiveRate);

        return result;
    }


}