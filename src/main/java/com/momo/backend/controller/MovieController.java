package com.momo.backend.controller;

import com.momo.backend.dto.review.ReviewResponse;
import com.momo.backend.service.MovieService;
import com.momo.backend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.momo.backend.dto.movie.MovieSearchResponse;
import java.util.List;
import com.momo.backend.dto.movie.MovieDetailResponse;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@CrossOrigin(origins="http://localhost:3000")
@Slf4j
public class MovieController {

    private final ReviewService reviewService;
    private final MovieService movieService;

    // 영화 제목 검색
//    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<String> searchMovies(
//            @RequestParam String keyword) {
//
//        String result = movieService.searchMovies(keyword);
//
//        return ResponseEntity.ok(result);
//    }
    // 영화 제목 검색
    @GetMapping("/search")
    public ResponseEntity<List<MovieSearchResponse>> searchMovies(
            @RequestParam String keyword) {

        return ResponseEntity.ok(movieService.searchMovies(keyword));
    }

    // 특정 영화의 리뷰 목록 조회 (기존 기능 유지)
    @GetMapping("/{movieCode}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getMovieReviews(
            @PathVariable String movieCode,
            @RequestParam(defaultValue = "0") int page) {

        return ResponseEntity.ok(
                reviewService.getMovieReviews(movieCode, page)
        );
    }

    // 영화 상세정보 조회
//    @GetMapping(value = "/{movieCode}", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<String> getMovieDetail(
//            @PathVariable String movieCode) {
//
//        String result = movieService.getMovieDetail(movieCode);
//
//        return ResponseEntity.ok(result);
//    }
    // 영화 상세정보 조회
    @GetMapping("/{movieCode}")
    public ResponseEntity<MovieDetailResponse> getMovieDetail(
            @PathVariable String movieCode) {

        return ResponseEntity.ok(movieService.getMovieDetail(movieCode));
    }
}