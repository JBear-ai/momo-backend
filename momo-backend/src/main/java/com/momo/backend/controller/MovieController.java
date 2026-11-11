package com.momo.backend.controller;

import com.momo.backend.dto.movie.MovieDetailResponse;
import com.momo.backend.dto.movie.MovieSearchPageResponse;
import com.momo.backend.dto.movie.MovieSearchResponse;
import com.momo.backend.dto.review.ReviewResponse;
import com.momo.backend.service.MovieService;
import com.momo.backend.service.ReviewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Slf4j
public class MovieController {

        private final ReviewService reviewService;
        private final MovieService movieService;

        @GetMapping("/recommendations")
        public ResponseEntity<List<Map<String, Object>>> getBoxOfficeRecommendations(
                        @RequestParam(required = false) String genre) {
                return ResponseEntity.ok(
                                movieService.getBoxOfficeRecommendations(genre));
        }

        @GetMapping("/search")
        public ResponseEntity<MovieSearchPageResponse> searchMovies(
                        @RequestParam String keyword,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "20") int size) {

                return ResponseEntity.ok(
                                movieService.searchMovies(keyword, page, size));
        }

        // KOBIS 영화 코드는 숫자뿐 아니라 영문자가 포함될 수 있다.
        @GetMapping("/{movieCode:[A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9]}/reviews")
        public ResponseEntity<Page<ReviewResponse>> getMovieReviews(
                        @PathVariable String movieCode,
                        @RequestParam(defaultValue = "0") int page) {

                return ResponseEntity.ok(
                                reviewService.getMovieReviews(movieCode, page));
        }

        @GetMapping("/{movieCode:[A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9]}/people")
        public ResponseEntity<List<Map<String, Object>>> getMoviePeople(
                        @PathVariable String movieCode) {
                return ResponseEntity.ok(movieService.getMoviePeople(movieCode));
        }

        @GetMapping("/{movieCode:[A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9]}")
        public ResponseEntity<MovieDetailResponse> getMovieDetail(
                        @PathVariable String movieCode) {

                return ResponseEntity.ok(
                                movieService.getMovieDetail(movieCode));
        }

        @GetMapping("/genre")
        public ResponseEntity<List<MovieSearchResponse>> getMoviesByGenre(
                        @RequestParam String genre,
                        @RequestParam(defaultValue = "1") int page) {

                return ResponseEntity.ok(
                                movieService.getMoviesByGenre(genre, page));
        }
}
