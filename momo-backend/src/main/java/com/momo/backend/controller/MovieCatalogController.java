package com.momo.backend.controller;

import com.momo.backend.dto.movie.MovieCatalogSyncStatusResponse;
import com.momo.backend.service.MovieCatalogSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movies/catalog")
@RequiredArgsConstructor
public class MovieCatalogController {

    private final MovieCatalogSyncService catalogSyncService;

    @GetMapping("/status")
    public ResponseEntity<MovieCatalogSyncStatusResponse> getStatus() {
        return ResponseEntity.ok(catalogSyncService.getStatus());
    }
}
