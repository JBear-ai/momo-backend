package com.momo.backend.controller;

import com.momo.backend.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.momo.backend.dto.movie.BoxOfficeResponse;
import java.util.List;


@RestController
@RequestMapping("/api/boxoffice")
@RequiredArgsConstructor
@CrossOrigin(origins="http://localhost:3000")
@Slf4j
public class BoxOfficeController {

    private final MovieService movieService;

    // 일별 박스오피스 조회
//    @GetMapping(value = "/daily", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<String> getDailyBoxOffice(
//            @RequestParam String targetDate) {
//
//        String result = movieService.getDailyBoxOffice(targetDate);
//
//        return ResponseEntity.ok(result);
//    }

    // 일별 박스오피스 조회
    @GetMapping("/daily")
    public ResponseEntity<List<BoxOfficeResponse>> getDailyBoxOffice(
            @RequestParam String targetDate) {

        return ResponseEntity.ok(movieService.getDailyBoxOffice(targetDate));
    }


}