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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoviePersistenceService {
    private final MovieRepository movieRepository;

    // 검색 결과에 포함된 영화 정보를 DB에 저장하거나 보완
    @Transactional
    public void saveSearchMovies(List<MovieSearchResponse> movies) {
        if (movies == null || movies.isEmpty()) {
            return;
        }

        // 같은 영화 코드가 한 응답에 중복으로 들어와도 신규 Entity를 한 번만 만든다.
        Map<String, MovieSearchResponse> uniqueMovies = new LinkedHashMap<>();
        for (MovieSearchResponse item : movies) {
            if (item == null
                    || item.movieCode() == null
                    || item.movieCode().isBlank()
                    || item.title() == null
                    || item.title().isBlank()) {
                continue;
            }

            MovieSearchResponse previous = uniqueMovies.putIfAbsent(item.movieCode(), item);
            if (previous != null) {
                log.debug("중복 영화 검색 결과 제거. movieCode={}", item.movieCode());
            }
        }

        if (uniqueMovies.isEmpty()) return;
        List<String> codes = new ArrayList<>(uniqueMovies.keySet());

        Map<String, Movie> existing = new HashMap<>();
        movieRepository.findAllById(codes).forEach(movie ->
                existing.put(movie.getMovieCode(), movie));
        List<Movie> inserts = new ArrayList<>();

        for (MovieSearchResponse item : uniqueMovies.values()) {
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