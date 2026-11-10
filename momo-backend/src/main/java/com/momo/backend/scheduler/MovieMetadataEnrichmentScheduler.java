package com.momo.backend.scheduler;

import com.momo.backend.entity.Movie;
import com.momo.backend.repository.MovieRepository;
import com.momo.backend.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MovieMetadataEnrichmentScheduler {

    private final MovieRepository movieRepository;
    private final MovieService movieService;

    // 외부 API 호출량을 제한하기 위해 60초마다 한 편만 처리한다.
    @Scheduled(fixedDelayString = "${momo.catalog.enrichment.delay-ms:60000}")
    public void enrichMissingMovieDetails() {
        List<Movie> movies = movieRepository.findMoviesNeedingMetadata(
                LocalDateTime.now().minusHours(6),
                PageRequest.of(0, 1)
        );

        for (Movie movie : movies) {
            try {
                movieService.getMovieDetail(movie.getMovieCode());
            } catch (Exception exception) {
                log.warn("영화 상세정보 자동 보완 실패. movieCode={}",
                        movie.getMovieCode(), exception);
            }
        }
    }
}
