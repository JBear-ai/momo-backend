
package com.momo.backend.service;

import com.momo.backend.dto.review.ReviewCreateRequest;
import com.momo.backend.dto.review.ReviewResponse;
import com.momo.backend.dto.review.ReviewPageResponse;
import com.momo.backend.dto.review.ReviewSummaryResponse;
import com.momo.backend.dto.review.ReviewUpdateRequest;

import com.momo.backend.entity.Review;
import com.momo.backend.entity.User;
import com.momo.backend.entity.Movie;

import com.momo.backend.repository.CommentReactionRepository;
import com.momo.backend.repository.CommentRepository;
import com.momo.backend.repository.MovieRepository;
import com.momo.backend.repository.ReviewReactionRepository;
import com.momo.backend.repository.ReviewRepository;
import com.momo.backend.repository.UserRepository;

import com.momo.backend.exception.CustomException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
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

    // 전체 리뷰 목록 조회

    @Transactional(readOnly = true)
    public Page<ReviewPageResponse> getAllReviews(
            int page,
            String searchType,
            String keyword) {

        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }

        Set<String> allowedSearchTypes = Set.of(
                "ALL", "MOVIE", "DIRECTOR", "ACTOR",
                "NICKNAME", "TITLE", "CONTENT"
        );
        String normalizedSearchType = searchType == null
                ? "ALL"
                : searchType.trim().toUpperCase();

        if (!allowedSearchTypes.contains(normalizedSearchType)) {
            throw new IllegalArgumentException("지원하지 않는 리뷰 검색 조건입니다.");
        }

        String normalizedKeyword = keyword == null
                ? ""
                : keyword.trim().replaceAll("\\s+", " ").toLowerCase();

        String pattern = normalizedKeyword.isEmpty()
                ? "%"
                : "%" + normalizedKeyword + "%";

        Pageable pageable = PageRequest.of(
                page,
                20,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("reviewId")
                )
        );

        Page<Review> reviews = normalizedKeyword.isEmpty()
                ? reviewRepository.findAllByOrderByCreatedAtDescReviewIdDesc(pageable)
                : reviewRepository.searchAllReviews(
                        normalizedSearchType,
                        pattern,
                        pageable
                );

        Set<String> movieCodes = reviews.getContent()
                .stream()
                .map(review -> review == null ? null : review.getMovieCode())
                .collect(Collectors.toSet());

        Map<String, Movie> movieMap =
                movieRepository.findAllById(movieCodes)
                        .stream()
                        .collect(Collectors.toMap(
                                movie -> movie == null ? null : movie.getMovieCode(),
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

    @Transactional(readOnly = true)
    public Page<ReviewPageResponse> getAllReviews(int page) {
        return getAllReviews(page, "ALL", "");
    }

    // 개봉일에서 연도 추출

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

    // 페이지에 표시할 댓글·좋아요·싫어요 집계를 두 번의 DB 조회로 가져온다.
    @Transactional(readOnly = true)
    public List<ReviewSummaryResponse> getReviewSummaries(List<Long> reviewIds) {
        if (reviewIds == null || reviewIds.isEmpty()) {
            return List.of();
        }
        if (reviewIds.size() > 50) {
            throw new IllegalArgumentException("한 번에 조회할 리뷰는 50개 이하여야 합니다.");
        }

        List<Long> ids = reviewIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> commentCounts = new HashMap<>();
        for (CommentRepository.ReviewCommentCountSummary summary
                : commentRepository.countByReviewIds(ids)) {
            commentCounts.put(summary.getReviewId(), summary.getCommentCount());
        }

        Map<Long, long[]> reactionCounts = new HashMap<>();
        for (ReviewReactionRepository.ReviewReactionCountSummary summary
                : reviewReactionRepository.findCountsByReviewIds(ids)) {
            reactionCounts.put(
                    summary.getReviewId(),
                    new long[]{
                            summary.getLikeCount() == null ? 0L : summary.getLikeCount(),
                            summary.getDislikeCount() == null ? 0L : summary.getDislikeCount()
                    }
            );
        }

        // 리뷰별 영화 코드와 영화 제목·개봉일도 한 번씩 모아 조회한다.
        Map<Long, Review> reviewsById = new HashMap<>();
        for (Review review : reviewRepository.findAllById(ids)) {
            reviewsById.put(review.getReviewId(), review);
        }

        List<String> movieCodes = reviewsById.values().stream()
                .map(Review::getMovieCode)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList();

        Map<String, Movie> moviesByCode = new HashMap<>();
        if (!movieCodes.isEmpty()) {
            for (Movie movie : movieRepository.findAllById(movieCodes)) {
                moviesByCode.put(movie.getMovieCode(), movie);
            }
        }

        List<ReviewSummaryResponse> result = new ArrayList<>(ids.size());
        for (Long reviewId : ids) {
            Review review = reviewsById.get(reviewId);
            Movie movie = review == null ? null : moviesByCode.get(review.getMovieCode());
            long[] counts = reactionCounts.getOrDefault(reviewId, new long[2]);
            result.add(new ReviewSummaryResponse(
                    reviewId,
                    review == null ? null : review.getMovieCode(),
                    movie == null ? null : movie.getTitle(),
                    movie == null ? null : movie.getReleaseDate(),
                    commentCounts.getOrDefault(reviewId, 0L),
                    counts[0],
                    counts[1]
            ));
        }
        return result;
    }

    // 리뷰 작성

    public ReviewResponse createReview(
            String loginId,
            ReviewCreateRequest dto) {

        User user = userRepository.findByLoginIdForUpdate(loginId)
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

        try {
            // 동시 작성 요청도 DB unique 제약조건으로 중복을 차단한다.
            Review savedReview = reviewRepository.saveAndFlush(review);
            return new ReviewResponse(savedReview);
        } catch (DataIntegrityViolationException duplicate) {
            throw new CustomException(
                    "DUPLICATE_REVIEW",
                    "이미 해당 영화에 리뷰를 작성했습니다."
            );
        }
    }

    // 리뷰 상세 조회

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

    // 영화별 리뷰 조회

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMovieReviews(
            String movieCode,
            int page) {

        Pageable pageable = PageRequest.of(page, 10);

        return reviewRepository
                .findByMovieCodeOrderByCreatedAtDescReviewIdDesc(
                        movieCode,
                        pageable
                )
                .map(ReviewResponse::new);
    }

    // 내가 작성한 리뷰 조회

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

    // 리뷰 수정

    public ReviewResponse updateReview(
            Long reviewId,
            String loginId,
            ReviewUpdateRequest dto) {

        User user = userRepository.findByLoginIdForUpdate(loginId)
                .orElseThrow(() -> new CustomException("USER_NOT_FOUND", "회원 정보를 찾을 수 없습니다."));

        // 리뷰 행 잠금은 새 댓글·리뷰 반응이 삭제 도중 추가되는 것을 막는다.
        Review review = reviewRepository.findByIdForUpdate(reviewId)
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

    // 리뷰 삭제

    public void deleteReview(
            Long reviewId,
            String loginId) {

        userRepository.findByLoginIdForUpdate(loginId)
                .orElseThrow(() -> new CustomException("USER_NOT_FOUND", "회원 정보를 찾을 수 없습니다."));

        // 리뷰 행 잠금으로 신규 댓글·반응 저장과 리뷰 삭제를 직렬화한다.
        Review review = reviewRepository.findByIdForUpdate(reviewId)
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
