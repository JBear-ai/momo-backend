package com.momo.backend.service;

import com.momo.backend.entity.Movie;
import com.momo.backend.entity.MoviePosterCache;
import com.momo.backend.entity.PosterStatus;
import com.momo.backend.repository.MoviePosterCacheRepository;
import com.momo.backend.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MoviePosterPersistenceService {

    private final MoviePosterCacheRepository posterCacheRepository;
    private final MovieRepository movieRepository;
    private final PosterFileStorageService posterFileStorageService;

    // TMDb 필모그래피에서 영화 ID로 정확히 확인한 포스터를 같은 KOBIS 영화 코드에 저장한다.
    // 기존 파일이나 진행 중인 포스터 작업은 덮어쓰지 않고 작업 소유권을 확인한다.
    public boolean saveFilmographyPosterIfMissing(String movieCode, String posterUrl) {
        if (!PosterFileStorageService.isValidMovieCode(movieCode)) {
            throw new IllegalArgumentException("올바르지 않은 영화 코드입니다.");
        }

        String normalizedPosterUrl = validatePosterUrl(posterUrl);
        Movie movie = movieRepository.findById(movieCode).orElse(null);
        if (movie == null) {
            return false;
        }

        if (isPosterFileAvailable(movie.getPosterUrl())) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        MoviePosterCache cache = posterCacheRepository.findById(movieCode).orElse(null);

        if (cache == null) {
            posterCacheRepository.insertIfAbsent(
                    movieCode,
                    movie.getTitle() == null ? "" : movie.getTitle(),
                    movie.getReleaseDate(),
                    50,
                    now
            );
            cache = posterCacheRepository.findById(movieCode).orElse(null);
        }

        if (cache == null) {
            return false;
        }

        if (cache.getStatus() == PosterStatus.SUCCESS
                && isPosterFileAvailable(cache.getPosterUrl())) {
            movie.updatePosterUrl(cache.getPosterUrl());
            movieRepository.save(movie);
            return true;
        }

        // 이미 검색 작업이 진행 중이면 그 작업의 claim token으로 TMDb에서 확인한 이미지를 확정한다.
        // 먼저 완료된 저장만 성공하고, 늦게 끝난 기존 작업은 completeClaim에서 자동으로 거부된다.
        if (cache.getStatus() == PosterStatus.PROCESSING) {
            String activeClaimToken = cache.getClaimToken();
            if (activeClaimToken == null || activeClaimToken.isBlank()) {
                return false;
            }
            return saveSuccess(movieCode, activeClaimToken, normalizedPosterUrl);
        }

        // 실패 대기 시간과 관계없이 영화 ID로 확인된 포스터를 우선 저장한다.
        posterCacheRepository.resetMissingPoster(
                movieCode,
                now,
                now.minusMinutes(10)
        );

        String claimToken = UUID.randomUUID().toString();
        int claimed = posterCacheRepository.tryClaim(
                movieCode,
                claimToken,
                now,
                now.minusMinutes(2),
                now.minusMinutes(10)
        );
        if (claimed != 1) {
            return false;
        }

        return saveSuccess(movieCode, claimToken, normalizedPosterUrl);
    }

    private boolean isPosterFileAvailable(String posterUrl) {
        return posterUrl != null
                && posterUrl.startsWith("/posters/")
                && posterFileStorageService.exists(posterUrl);
    }

    public boolean saveSuccess(String movieCode, String claimToken, String posterUrl) {
        String normalizedPosterUrl = validatePosterUrl(posterUrl);
        String localPosterPath = posterFileStorageService.downloadAndSave(
                movieCode, normalizedPosterUrl);

        int completed = posterCacheRepository.completeClaim(
                movieCode, claimToken, localPosterPath, LocalDateTime.now());
        if (completed != 1) {
            // 작업 소유권이 바뀐 경우 오래된 비동기 작업이 남긴 파일을 정리한다.
            posterFileStorageService.delete(localPosterPath);
            return false;
        }

        movieRepository.findById(movieCode).ifPresent(movie -> {
            movie.updatePosterUrl(localPosterPath);
            movieRepository.save(movie);
        });
        return true;
    }

    // 외부 공급원 응답에 이미지 주소가 아닌 값이나 사용자 정보가 포함된 URI가 섞이지 않도록 검사한다.
    private String validatePosterUrl(String posterUrl) {
        if (posterUrl == null || posterUrl.isBlank() || posterUrl.length() > 1000) {
            throw new IllegalArgumentException("유효하지 않은 포스터 주소입니다.");
        }

        try {
            URI uri = new URI(posterUrl.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (scheme == null
                    || host == null
                    || uri.getUserInfo() != null
                    || !(scheme.equalsIgnoreCase("https")
                    || scheme.equalsIgnoreCase("http"))) {
                throw new IllegalArgumentException("유효하지 않은 포스터 주소입니다.");
            }

            String normalizedHost = host.toLowerCase(Locale.ROOT);
            if (normalizedHost.equals("localhost")
                    || normalizedHost.endsWith(".localhost")
                    || normalizedHost.equals("127.0.0.1")
                    || normalizedHost.equals("0.0.0.0")
                    || normalizedHost.equals("::1")) {
                throw new IllegalArgumentException("허용되지 않는 포스터 주소입니다.");
            }
            return uri.toString();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("유효하지 않은 포스터 주소입니다.", exception);
        }
    }
}
