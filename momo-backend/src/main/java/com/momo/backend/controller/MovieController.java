package com.momo.backend.controller;

import com.momo.backend.dto.movie.MovieDetailResponse;
import com.momo.backend.dto.movie.MovieSearchPageResponse;
import com.momo.backend.dto.movie.MovieSearchResponse;
import com.momo.backend.dto.review.ReviewResponse;
import com.momo.backend.service.MovieService;
import com.momo.backend.service.PersonService;
import com.momo.backend.service.TmdbService;
import com.momo.backend.service.ReviewService;
import org.springframework.web.server.ResponseStatusException;

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
        private final PersonService personService;
        private final TmdbService tmdbService;

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

        // TMDb ID로 진입하고, KOBIS에 연결되면 기존 영화 기능을 그대로 사용한다.
        @GetMapping("/tmdb/{tmdbId}")
        public ResponseEntity<?> getMovieDetailByTmdbId(@PathVariable long tmdbId) {
                if (tmdbId <= 0) {
                        throw new ResponseStatusException(
                                        org.springframework.http.HttpStatus.BAD_REQUEST,
                                        "올바르지 않은 TMDb 영화 ID입니다.");
                }

                String movieCode = null;
                try {
                        movieCode = personService.resolveFilmographyMovie(tmdbId);
                } catch (ResponseStatusException mappingError) {
                        // KOBIS 미등록·동명 후보·외부 검색 장애가 TMDb 상세 화면까지 막지 않게 한다.
                        if (mappingError.getStatusCode().value() == 400) {
                                throw mappingError;
                        }
                        log.info("KOBIS 영화 코드 미연결. TMDb 상세정보로 표시합니다. tmdbId={}, status={}",
                                        tmdbId, mappingError.getStatusCode().value());
                }

                if (movieCode != null && movieCode.matches("[A-Za-z0-9]{8}")) {
                        return ResponseEntity.ok(movieService.getMovieDetail(movieCode));
                }

                return ResponseEntity.ok(tmdbService.getMovieDetailById(tmdbId));
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
