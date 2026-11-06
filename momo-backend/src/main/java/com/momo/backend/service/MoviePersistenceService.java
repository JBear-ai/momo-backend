package com.momo.backend.service;

import com.momo.backend.dto.movie.MovieSearchResponse;
import com.momo.backend.entity.Movie;
import com.momo.backend.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoviePersistenceService {
    private final MovieRepository movieRepository;

    @Transactional
    public void saveSearchMovies(List<MovieSearchResponse> movies) {
        if (movies == null || movies.isEmpty()) {
            return;
        }

        List<String> codes = movies.stream()
                .filter(item -> item != null)
                .map(MovieSearchResponse::movieCode)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList();
        if (codes.isEmpty()) return;

        Map<String, Movie> existing = new HashMap<>();
        movieRepository.findAllById(codes).forEach(movie ->
                existing.put(movie.getMovieCode(), movie));
        List<Movie> inserts = new ArrayList<>();

        for (MovieSearchResponse item : movies) {
            if (item == null || item.movieCode() == null || item.movieCode().isBlank()
                    || item.title() == null || item.title().isBlank()) {
                continue;
            }
            Movie saved = existing.get(item.movieCode());
            if (saved == null) {
                inserts.add(new Movie(
                        item.movieCode(),
                        item.title(),
                        "",
                        item.releaseDate(),
                        "",
                        item.genre(),
                        item.director(),
                        "",
                        ""
                ));
            } else {
                saved.supplementMissingDetails(
                        "",
                        item.releaseDate(),
                        "",
                        item.genre(),
                        item.director(),
                        "",
                        "",
                        "KOBIS_SEARCH"
                );
            }
        }
        if (!inserts.isEmpty()) {
            movieRepository.saveAll(inserts);
        }
        log.info("검색 영화 DB 저장 완료. 신규 저장: {}, 기존 영화: {}", inserts.size(), existing.size());
    }
}