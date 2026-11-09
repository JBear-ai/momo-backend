package com.momo.backend.service;

import com.momo.backend.repository.MoviePosterCacheRepository;
import com.momo.backend.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MoviePosterPersistenceService {

    private final MoviePosterCacheRepository posterCacheRepository;
    private final MovieRepository movieRepository;

    @Transactional
    public void saveSuccess(String movieCode, String posterUrl) {
        String normalizedPosterUrl = validatePosterUrl(posterUrl);

        posterCacheRepository.findById(movieCode)
                .ifPresent(cache -> cache.updatePoster(normalizedPosterUrl));
        movieRepository.findById(movieCode)
                .ifPresent(movie -> movie.updatePosterUrl(normalizedPosterUrl));
    }

    // 외부 공급원 응답에 이미지 주소가 아닌 값이나 위험한 URI가 섞이는 것을 방지한다.
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
