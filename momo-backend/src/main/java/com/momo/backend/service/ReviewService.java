package com.momo.backend.service;

import com.momo.backend.dto.review.ReviewCreateRequest;
import com.momo.backend.dto.review.ReviewResponse;
import com.momo.backend.dto.review.ReviewPageResponse;
import com.momo.backend.dto.review.ReviewUpdateRequest;

import com.momo.backend.entity.Review;
import com.momo.backend.entity.User;
import com.momo.backend.entity.Movie;

import com.momo.backend.repository.*;

import com.momo.backend.exception.CustomException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ReviewService {

    // 리뷰 및 회원 DB 접근
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    // 영화 DB 접근
    private final MovieRepository movieRepository;

    // 리뷰 삭제 시 해당 리뷰의 댓글도 함께 삭제
    private final CommentRepository commentRepository;

    // 리뷰 및 댓글 좋아요 삭제
    private final ReviewReactionRepository reviewReactionRepository;
    private final CommentReactionRepository commentReactionRepository;


    // =========================================================
    // 1. 전체 리뷰 목록 조회
    // =========================================================

    @Transactional(readOnly = true)
    public Page<ReviewPageResponse> getAllReviews(int page) {

        // 한 페이지에 리뷰 20개, 최신 작성순 정렬
        Pageable pageable = PageRequest.of(
                page,
                20,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("reviewId")
                )
        );

        // 모든 영화의 리뷰를 페이지 단위로 조회
        Page<Review> reviews =
                reviewRepository.findAllByOrderByCreatedAtDescReviewIdDesc(
                        pageable
                );

        // 현재 페이지에 포함된 영화 코드만 수집
        Set<String> movieCodes = reviews.getContent()
                .stream()
                .map(Review::getMovieCode)
                .collect(Collectors.toSet());

        // 영화 정보를 한 번에 조회
        Map<String, Movie> movieMap =
                movieRepository.findAllById(movieCodes)
                        .stream()
                        .collect(Collectors.toMap(
                                Movie::getMovieCode,
                                movie -> movie
                        ));

        // 리뷰와 영화 정보를 결합하여 목록 DTO로 변환
        return reviews.map(review -> {

            Movie movie = movieMap.get(review.getMovieCode());

            // 영화 정보가 없는 경우 기본값 처리
            if (movie == null) {
                return new ReviewPageResponse(
                        review,
                        "알 수 없는 영화",
                        null
                );
            }

            // 개봉연도 추출
            Integer movieYear = extractMovieYear(
                    movie.getReleaseDate()
            );

            return new ReviewPageResponse(
                    review,
                    movie.getTitle(),
                    movieYear
            );
        });
    }


    // =========================================================
    // 2. 개봉일에서 연도 추출
    // =========================================================

    private Integer extractMovieYear(String releaseDate) {

        if (releaseDate == null || releaseDate.length() < 4) {
            return null;
        }

        try {
            return Integer.parseInt(
                    releaseDate.substring(0, 4)
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }


    // =========================================================
    // 3. 리뷰 작성
    // =========================================================

    public ReviewResponse createReview(
            String loginId,
            ReviewCreateRequest dto) {

        // 로그인 아이디로 작성자 조회
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new CustomException(
                                "USER_NOT_FOUND",
                                "회원 정보를 찾을 수 없습니다."));

        // 같은 영화에 이미 리뷰를 작성했는지 확인
        boolean exists = reviewRepository
                .existsByUser_UserIdAndMovieCode(
                        user.getUserId(),
                        dto.getMovieCode()
                );

        if (exists) {
            throw new CustomException(
                    "DUPLICATE_REVIEW",
                    "이미 해당 영화에 리뷰를 작성했습니다.");
        }

        // 리뷰 Entity 생성
        Review review = new Review(
                user,
                dto.getMovieCode(),
                dto.getTitle(),
                dto.getContent(),
                dto.getLiked()
        );

        // 리뷰 저장
        Review savedReview = reviewRepository.save(review);

        return new ReviewResponse(savedReview);
    }


    // =========================================================
    // 4. 리뷰 상세 조회
    // =========================================================

    @Transactional(readOnly = true)
    public ReviewResponse getReview(Long reviewId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new CustomException(
                                "REVIEW_NOT_FOUND",
                                "리뷰를 찾을 수 없습니다."));

        return new ReviewResponse(review);
    }


    // =========================================================
    // 5. 영화별 리뷰 조회
    // =========================================================

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMovieReviews(
            String movieCode,
            int page) {

        Pageable pageable = PageRequest.of(page, 10);

        return reviewRepository
                .findByMovieCodeOrderByCreatedAtDesc(
                        movieCode,
                        pageable
                )
                .map(ReviewResponse::new);
    }


    // =========================================================
    // 6. 내가 작성한 리뷰 조회
    // =========================================================

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyReviews(
            String loginId,
            int page) {

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new CustomException(
                                "USER_NOT_FOUND",
                                "회원 정보를 찾을 수 없습니다."));

        Pageable pageable = PageRequest.of(
                page,
                10,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt",
                        "reviewId"
                )
        );

        return reviewRepository
                .findByUser_UserId(
                        user.getUserId(),
                        pageable
                )
                .map(ReviewResponse::new);
    }


    // =========================================================
    // 7. 리뷰 수정
    // =========================================================

    public ReviewResponse updateReview(
            Long reviewId,
            String loginId,
            ReviewUpdateRequest dto) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new CustomException(
                                "REVIEW_NOT_FOUND",
                                "리뷰를 찾을 수 없습니다."));

        // 작성자 확인
        if (!review.getUser().getLoginId().equals(loginId)) {
            throw new CustomException(
                    "FORBIDDEN",
                    "본인이 작성한 리뷰만 수정할 수 있습니다.");
        }

        // 리뷰 수정
        review.updateReview(
                dto.getTitle(),
                dto.getContent(),
                dto.getLiked()
        );

        return new ReviewResponse(review);
    }


    // =========================================================
    // 8. 리뷰 삭제
    // =========================================================

    public void deleteReview(
            Long reviewId,
            String loginId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new CustomException(
                                "REVIEW_NOT_FOUND",
                                "리뷰를 찾을 수 없습니다."));

        // 작성자 확인
        if (!review.getUser().getLoginId().equals(loginId)) {
            throw new CustomException(
                    "FORBIDDEN",
                    "본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }

        // 댓글 반응 삭제
        commentReactionRepository
                .deleteByComment_Review_ReviewId(reviewId);

        // 댓글 삭제
        commentRepository.deleteByReview_ReviewId(reviewId);

        // 리뷰 반응 삭제
        reviewReactionRepository
                .deleteByReview_ReviewId(reviewId);

        // 리뷰 삭제
        reviewRepository.delete(review);
    }


    // =========================================================
    // 9. 영화별 좋아요 / 싫어요 통계
    // =========================================================

    @Transactional(readOnly = true)
    public Map<String, Object> getMovieRating(String movieCode) {

        long likeCount = reviewRepository
                .countByMovieCodeAndLiked(movieCode, true);

        long dislikeCount = reviewRepository
                .countByMovieCodeAndLiked(movieCode, false);

        long totalCount = likeCount + dislikeCount;

        Double positiveRate = null;

        if (totalCount > 0) {

            positiveRate = (double) likeCount / totalCount * 100;

            positiveRate = Math.round(
                    positiveRate * 10
            ) / 10.0;
        }

        Map<String, Object> result = new HashMap<>();

        result.put("movieCode", movieCode);
        result.put("likeCount", likeCount);
        result.put("dislikeCount", dislikeCount);
        result.put("totalCount", totalCount);
        result.put("positiveRate", positiveRate);

        return result;
    }

}