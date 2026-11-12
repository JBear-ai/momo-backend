package com.momo.backend.dto.favorite;

import com.momo.backend.entity.FavoriteMovie;
import java.time.LocalDateTime;

public record FavoriteMovieResponse(
        String movieCode,
        String title,
        String releaseDate,
        String posterUrl,
        LocalDateTime createdAt
) {
    public FavoriteMovieResponse(FavoriteMovie favorite) {
        this(
                favorite.getMovieCode(),
                favorite.getTitle(),
                favorite.getReleaseDate(),
                favorite.getPosterUrl(),
                favorite.getCreatedAt()
        );
    }
}
