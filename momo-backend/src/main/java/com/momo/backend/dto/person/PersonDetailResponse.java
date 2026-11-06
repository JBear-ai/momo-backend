package com.momo.backend.dto.person;

import java.util.List;

public record PersonDetailResponse(
        PersonInfo personInfo,
        List<FilmographyItem> filmography
) {
    public record PersonInfo(
            long tmdbId,
            String name,
            String profileUrl,
            String birthday,
            String deathday,
            String placeOfBirth,
            String biography,
            String knownForDepartment
    ) {
    }

    public record FilmographyItem(
            long tmdbId,
            String title,
            String releaseDate,
            String posterUrl,
            String role
    ) {
    }
}