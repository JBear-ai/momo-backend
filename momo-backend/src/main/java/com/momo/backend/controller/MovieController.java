
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

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Slf4j
public class MovieController {

    private final ReviewService reviewService;
    private final MovieService movieService;

    // =========================================================
    // 1. 영화 제목 검색 (페이지네이션)
    // =========================================================

    @GetMapping("/search")
    public ResponseEntity<MovieSearchPageResponse> searchMovies(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                movieService.searchMovies(keyword, page, size)
        );
    }

    // =========================================================
    // 2. 특정 영화의 리뷰 목록 조회
    // =========================================================

    @GetMapping("/{movieCode}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getMovieReviews(
            @PathVariable String movieCode,
            @RequestParam(defaultValue = "0") int page) {

        return ResponseEntity.ok(
                reviewService.getMovieReviews(movieCode, page)
        );
    }

    // =========================================================
    // 3. 영화 상세정보 조회
    // =========================================================

    @GetMapping("/{movieCode}")
    public ResponseEntity<MovieDetailResponse> getMovieDetail(
            @PathVariable String movieCode) {

        return ResponseEntity.ok(
                movieService.getMovieDetail(movieCode)
        );
    }

    // =========================================================
    // 4. 장르별 영화 목록 조회
    // =========================================================

    @GetMapping("/genre")
    public ResponseEntity<List<MovieSearchResponse>> getMoviesByGenre(
            @RequestParam String genre,
            @RequestParam(defaultValue = "1") int page) {

        return ResponseEntity.ok(
                movieService.getMoviesByGenre(genre, page)
        );
    }
}
