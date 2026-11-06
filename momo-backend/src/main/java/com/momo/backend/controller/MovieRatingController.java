package com.momo.backend.controller;

import com.momo.backend.service.MovieRatingService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieRatingController {

        private final MovieRatingService movieRatingService;

        @GetMapping("/{movieCode:[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]}/rating")
        public ResponseEntity<Map<String, Object>> getMovieRating(
                        @PathVariable String movieCode,
                        Authentication authentication) {

                String loginId = authentication != null
                                ? authentication.getName()
                                : null;

                Map<String, Object> result = movieRatingService.getMovieRating(
                                movieCode,
                                loginId);

                return ResponseEntity.ok(result);
        }

        @PutMapping("/{movieCode:[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]}/rating")
        public ResponseEntity<Void> saveMovieRating(
                        @PathVariable String movieCode,
                        @RequestBody Boolean liked,
                        Authentication authentication) {

                movieRatingService.saveRating(
                                authentication.getName(),
                                movieCode,
                                liked);

                return ResponseEntity.ok().build();
        }

        @DeleteMapping("/{movieCode:[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]}/rating")
        public ResponseEntity<Void> deleteMovieRating(
                        @PathVariable String movieCode,
                        Authentication authentication) {

                movieRatingService.deleteRating(
                                authentication.getName(),
                                movieCode);

                return ResponseEntity.noContent().build();
        }
}
