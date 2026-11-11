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

        // TMDb ID로 상세정보와 인물 정보를 확보하고, KOBIS 연결은 기존 커뮤니티 기능에 재사용한다.
        @GetMapping("/tmdb/{tmdbId}")
        public ResponseEntity<?> getMovieDetailByTmdbId(@PathVariable long tmdbId) {
                if (tmdbId <= 0) {
                        throw new ResponseStatusException(
                                        org.springframework.http.HttpStatus.BAD_REQUEST,
                                        "올바르지 않은 TMDb 영화 ID입니다.");
                }

                // KOBIS 매칭 결과와 관계없이 원본 TMDb 정보를 먼저 확보해 상세 화면의 기준으로 사용한다.
                TmdbMovieDetailResponse tmdbDetail = tmdbService.getMovieDetailById(tmdbId);
                String movieCode = null;

                try {
                        movieCode = personService.resolveFilmographyMovie(tmdbId);
                } catch (ResponseStatusException mappingError) {
                        // KOBIS 미등록·동명 후보·외부 검색 장애가 영화 상세정보와 인물 표시를 막지 않게 한다.
                        if (mappingError.getStatusCode().value() == 400) {
                                throw mappingError;
                        }
                        log.info("KOBIS 영화 코드 미연결. TMDb 상세정보로 표시합니다. tmdbId={}, status={}",
                                        tmdbId, mappingError.getStatusCode().value());
                }

                if (movieCode == null || !movieCode.matches("[A-Za-z0-9]{8}")) {
                        return ResponseEntity.ok(tmdbDetail);
                }

                try {
                        MovieDetailResponse kobisDetail = movieService.getMovieDetail(movieCode);
                        return ResponseEntity.ok(mergeMovieDetails(tmdbDetail, kobisDetail, movieCode));
                } catch (RuntimeException detailError) {
                        // KOBIS 상세정보가 불완전해도 TMDb 상세정보와 인물 연결은 유지한다.
                        log.warn("KOBIS 상세정보 보완 실패. TMDb 상세정보를 유지합니다. tmdbId={}, movieCode={}",
                                        tmdbId, movieCode, detailError);
                        return ResponseEntity.ok(withMovieCode(tmdbDetail, movieCode));
                }
        }

        private TmdbMovieDetailResponse mergeMovieDetails(
                        TmdbMovieDetailResponse tmdb,
                        MovieDetailResponse kobis,
                        String movieCode) {
                List<String> actors = kobis.actors() != null && !kobis.actors().isEmpty()
                                ? kobis.actors()
                                : tmdb.actors();

                return new TmdbMovieDetailResponse(
                                tmdb.tmdbId(),
                                movieCode,
                                firstNonBlank(kobis.title(), tmdb.title()),
                                firstNonBlank(kobis.titleEn(), tmdb.titleEn()),
                                firstNonBlank(kobis.releaseDate(), tmdb.releaseDate()),
                                firstNonBlank(kobis.runningTime(), tmdb.runningTime()),
                                firstNonBlank(kobis.genre(), tmdb.genre()),
                                firstNonBlank(kobis.director(), tmdb.director()),
                                actors,
                                firstNonBlank(kobis.watchGrade(), tmdb.watchGrade()),
                                firstNonBlank(kobis.overview(), tmdb.overview()),
                                tmdb.posterUrl(),
                                tmdb.people());
        }

        private TmdbMovieDetailResponse withMovieCode(
                        TmdbMovieDetailResponse tmdb,
                        String movieCode) {
                return new TmdbMovieDetailResponse(
                                tmdb.tmdbId(),
                                movieCode,
                                tmdb.title(),
                                tmdb.titleEn(),
                                tmdb.releaseDate(),
                                tmdb.runningTime(),
                                tmdb.genre(),
                                tmdb.director(),
                                tmdb.actors(),
                                tmdb.watchGrade(),
                                tmdb.overview(),
                                tmdb.posterUrl(),
                                tmdb.people());
        }

        private String firstNonBlank(String primary, String fallback) {
                return primary != null && !primary.isBlank() ? primary : fallback;
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
