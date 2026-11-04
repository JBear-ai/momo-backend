
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

        User user = userRepository.findByLoginId(loginId)
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
            movieRatingRepository.save(
                    new MovieRating(user, movie, liked)
            );
        } else {
            rating.updateRating(liked);
        }
    }

    // 로그인한 회원의 영화 평가 취소
    public void deleteRating(
            String loginId,
            String movieCode
    ) {

        User user = userRepository.findByLoginId(loginId)
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

    // 특정 영화의 전체 평가 및 로그인 회원의 평가 상태 조회
    @Transactional(readOnly = true)
    public Map<String, Object> getMovieRating(
            String movieCode,
            String loginId
    ) {

        // 영화 존재 여부 확인
        if (!movieRepository.existsById(movieCode)) {
            throw new CustomException(
                    "MOVIE_NOT_FOUND",
                    "영화 정보를 찾을 수 없습니다."
            );
        }

        // 좋아요 및 싫어요 개수 조회
        long likeCount = movieRatingRepository
                .countByMovie_MovieCodeAndLiked(movieCode, true);

        long dislikeCount = movieRatingRepository
                .countByMovie_MovieCodeAndLiked(movieCode, false);

        long totalCount = likeCount + dislikeCount;

        // 긍정 평가 비율 계산
        Double positiveRate = null;

        if (totalCount > 0) {
            positiveRate = Math.round(
                    (double) likeCount / totalCount * 1000
            ) / 10.0;
        }

        // 로그인하지 않은 회원의 평가 상태는 null
        Boolean myRating = null;

        // 로그인한 회원이면 기존 평가 확인
        if (loginId != null) {

            User user = userRepository.findByLoginId(loginId)
                    .orElse(null);

            if (user != null) {
                myRating = movieRatingRepository
                        .findByUser_UserIdAndMovie_MovieCode(
                                user.getUserId(),
                                movieCode
                        )
                        .map(MovieRating::getLiked)
                        .orElse(null);
            }
        }

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
