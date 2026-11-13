package com.momo.backend.service;

import com.momo.backend.entity.Movie;
import com.momo.backend.entity.MovieRating;
import com.momo.backend.entity.User;
import com.momo.backend.exception.CustomException;
import com.momo.backend.repository.MovieRatingRepository;
import com.momo.backend.repository.MovieRepository;
import com.momo.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class MovieRatingService {

    private final MovieRatingRepository movieRatingRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;

    // 영화 좋아요 또는 싫어요 등록 및 변경
    public void saveRating(
            String loginId,
            String movieCode,
            Boolean liked
    ) {

        if (liked == null) {
            throw new IllegalArgumentException(
                    "좋아요 또는 싫어요를 선택해주세요."
            );
        }

        // 같은 회원의 같은 영화 평가 저장·변경·삭제를 직렬화한다.
        User user = userRepository.findByLoginIdForUpdate(loginId)
                .orElseThrow(() ->
                        new CustomException(
                                "USER_NOT_FOUND",
                                "회원 정보를 찾을 수 없습니다."
                        )
                );

        Movie movie = movieRepository.findById(movieCode)
                .orElseThrow(() ->
                        new CustomException(
                                "MOVIE_NOT_FOUND",
                                "영화 정보를 찾을 수 없습니다."
                        )
                );

        MovieRating rating = movieRatingRepository
                .findByUser_UserIdAndMovie_MovieCode(
                        user.getUserId(),
                        movieCode
                )
                .orElse(null);

        if (rating == null) {
            // 사용자 행 잠금으로 같은 회원의 동시 INSERT를 직렬화한다.
            movieRatingRepository.save(new MovieRating(user, movie, liked));
        } else {
            rating.updateRating(liked);
        }
    }

    // 로그인한 회원의 영화 평가 취소
    public void deleteRating(
            String loginId,
            String movieCode
    ) {

        User user = userRepository.findByLoginIdForUpdate(loginId)
                .orElseThrow(() ->
                        new CustomException(
                                "USER_NOT_FOUND",
                                "회원 정보를 찾을 수 없습니다."
                        )
                );

        MovieRating rating = movieRatingRepository
                .findByUser_UserIdAndMovie_MovieCode(
                        user.getUserId(),
                        movieCode
                )
                .orElseThrow(() ->
                        new CustomException(
                                "RATING_NOT_FOUND",
                                "취소할 영화 평가가 없습니다."
                        )
                );

        movieRatingRepository.delete(rating);
    }

    // 로그인 회원이 좋아요한 영화 목록 조회
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyLikedMovies(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            return List.of();
        }

        User user = userRepository.findByLoginId(loginId).orElse(null);
        if (user == null) {
            return List.of();
        }

        return movieRatingRepository
                .findByUser_UserIdAndLikedOrderByRatingIdDesc(user.getUserId(), true)
                .stream()
                .map(rating -> {
                    Movie movie = rating.getMovie();
                    Map<String, Object> item = new HashMap<>();
                    item.put("movieCode", movie.getMovieCode());
                    item.put("title", movie.getTitle());
                    item.put("genre", movie.getGenre());
                    item.put("releaseDate", movie.getReleaseDate());
                    item.put("posterUrl", movie.getPosterUrl());
                    return item;
                })
                .toList();
    }

    // 특정 영화의 전체 평가 및 로그인 회원의 평가 상태 조회
    @Transactional(readOnly = true)
    public Map<String, Object> getMovieRating(
            String movieCode,
            String loginId
    ) {

        if (!movieRepository.existsById(movieCode)) {
            throw new CustomException(
                    "MOVIE_NOT_FOUND",
                    "영화 정보를 찾을 수 없습니다."
            );
        }

        // 좋아요·싫어요 수와 로그인 회원의 반응을 단일 집계 쿼리로 조회한다.
        // 기존의 전체 집계 + 회원 조회 + 회원별 평가 조회(최대 3회)를 1회로 줄인다.
        MovieRatingRepository.MovieRatingResponseSummary summary =
                movieRatingRepository.findResponseSummaryByMovieCode(
                        movieCode,
                        loginId == null || loginId.isBlank() ? null : loginId
                );

        long likeCount = summary == null ? 0L : summary.getLikeCount();
        long dislikeCount = summary == null ? 0L : summary.getDislikeCount();
        long totalCount = likeCount + dislikeCount;

        Double positiveRate = totalCount == 0
                ? null
                : Math.round((double) likeCount / totalCount * 1000) / 10.0;

        Integer myRatingValue = summary == null ? null : summary.getMyRating();
        Boolean myRating = myRatingValue == null ? null : myRatingValue == 1;

        Map<String, Object> result = new HashMap<>();
        result.put("movieCode", movieCode);
        result.put("likeCount", likeCount);
        result.put("dislikeCount", dislikeCount);
        result.put("totalCount", totalCount);
        result.put("positiveRate", positiveRate);
        result.put("myRating", myRating);

        return result;
    }
}
