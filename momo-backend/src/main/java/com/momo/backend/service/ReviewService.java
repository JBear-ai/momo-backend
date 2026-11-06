
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;

    private final CommentRepository commentRepository;
    private final ReviewReactionRepository reviewReactionRepository;
    private final CommentReactionRepository commentReactionRepository;


    // 1. 전체 리뷰 목록 조회

    @Transactional(readOnly = true)
    public Page<ReviewPageResponse> getAllReviews(int page) {

        Pageable pageable = PageRequest.of(
                page,
                20,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("reviewId")
                )
        );

        Page<Review> reviews =
                reviewRepository.findAllByOrderByCreatedAtDescReviewIdDesc(
                        pageable
                );

        Set<String> movieCodes = reviews.getContent()
                .stream()
                .map(Review::getMovieCode)
                .collect(Collectors.toSet());

        Map<String, Movie> movieMap =
                movieRepository.findAllById(movieCodes)
                        .stream()
                        .collect(Collectors.toMap(
                                Movie::getMovieCode,
                                movie -> movie
                        ));

        return reviews.map(review -> {

            Movie movie = movieMap.get(review.getMovieCode());

            if (movie == null) {
                return new ReviewPageResponse(
                        review,
                        "알 수 없는 영화",
                        null
                );
            }

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


    // 2. 개봉일에서 연도 추출

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


    // 3. 리뷰 작성

    public ReviewResponse createReview(
            String loginId,
            ReviewCreateRequest dto) {

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new CustomException(
                                "USER_NOT_FOUND",
                                "회원 정보를 찾을 수 없습니다."
                        ));

        boolean exists = reviewRepository
                .existsByUser_UserIdAndMovieCode(
                        user.getUserId(),
                        dto.getMovieCode()
                );

        if (exists) {
            throw new CustomException(
                    "DUPLICATE_REVIEW",
                    "이미 해당 영화에 리뷰를 작성했습니다."
            );
        }

        Review review = new Review(
                user,
                dto.getMovieCode(),
                dto.getTitle(),
                dto.getContent()
        );

        Review savedReview = reviewRepository.save(review);

        return new ReviewResponse(savedReview);
    }


    // 4. 리뷰 상세 조회

    @Transactional(readOnly = true)
    public ReviewResponse getReview(Long reviewId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new CustomException(
                                "REVIEW_NOT_FOUND",
                                "리뷰를 찾을 수 없습니다."
                        ));

        return new ReviewResponse(review);
    }


    // 5. 영화별 리뷰 조회

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


    // 6. 내가 작성한 리뷰 조회

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyReviews(
            String loginId,
            int page) {

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new CustomException(
                                "USER_NOT_FOUND",
                                "회원 정보를 찾을 수 없습니다."
                        ));

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


    // 7. 리뷰 수정

    public ReviewResponse updateReview(
            Long reviewId,
            String loginId,
            ReviewUpdateRequest dto) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new CustomException(
                                "REVIEW_NOT_FOUND",
                                "리뷰를 찾을 수 없습니다."
                        ));

        if (!review.getUser().getLoginId().equals(loginId)) {
            throw new CustomException(
                    "FORBIDDEN",
                    "본인이 작성한 리뷰만 수정할 수 있습니다."
            );
        }

        review.updateReview(
                dto.getTitle(),
                dto.getContent()
        );

        return new ReviewResponse(review);
    }


    // 8. 리뷰 삭제

    public void deleteReview(
            Long reviewId,
            String loginId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new CustomException(
                                "REVIEW_NOT_FOUND",
                                "리뷰를 찾을 수 없습니다."
                        ));

        if (!review.getUser().getLoginId().equals(loginId)) {
            throw new CustomException(
                    "FORBIDDEN",
                    "본인이 작성한 리뷰만 삭제할 수 있습니다."
            );
        }

        commentReactionRepository
                .deleteByComment_Review_ReviewId(reviewId);

        commentRepository.deleteByReview_ReviewId(reviewId);

        reviewReactionRepository
                .deleteByReview_ReviewId(reviewId);

        reviewRepository.delete(review);
    }
}
