package com.momo.backend.controller;

import com.momo.backend.dto.favorite.FavoriteMovieRequest;
import com.momo.backend.dto.favorite.FavoriteMovieResponse;
import com.momo.backend.service.FavoriteMovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteMovieController {

    private final FavoriteMovieService favoriteMovieService;

    @GetMapping
    public ResponseEntity<List<FavoriteMovieResponse>> getFavorites(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                favoriteMovieService.getFavorites(authentication.getName())
        );
    }

    @PostMapping
    public ResponseEntity<FavoriteMovieResponse> addFavorite(
            @Valid @RequestBody FavoriteMovieRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                favoriteMovieService.addFavorite(authentication.getName(), request)
        );
    }

    @DeleteMapping("/{movieCode}")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable String movieCode,
            Authentication authentication
    ) {
        favoriteMovieService.removeFavorite(authentication.getName(), movieCode);
        return ResponseEntity.noContent().build();
    }
}
