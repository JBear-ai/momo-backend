package com.momo.backend.service;

import com.momo.backend.repository.MoviePosterCacheRepository;
import com.momo.backend.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MoviePosterPersistenceService {
    private final MoviePosterCacheRepository posterCacheRepository;
    private final MovieRepository movieRepository;

    @Transactional
    public void saveSuccess(String movieCode, String posterUrl) {
        posterCacheRepository.findById(movieCode).ifPresent(cache -> cache.updatePoster(posterUrl));
        movieRepository.findById(movieCode).ifPresent(movie -> movie.updatePosterUrl(posterUrl));
    }
}