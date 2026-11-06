package com.momo.backend.controller;

import com.momo.backend.dto.movie.BoxOfficeResponse;
import com.momo.backend.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/boxoffice")
@RequiredArgsConstructor
public class BoxOfficeController {

    private final MovieService movieService;

    // 일별 박스오피스 조회
    @GetMapping("/daily")
    public ResponseEntity<List<BoxOfficeResponse>> getDailyBoxOffice(
            @RequestParam String targetDate) {

        return ResponseEntity.ok(movieService.getDailyBoxOffice(targetDate));
    }

}