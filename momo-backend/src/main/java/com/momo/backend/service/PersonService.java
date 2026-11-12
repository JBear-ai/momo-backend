package com.momo.backend.service;

import com.momo.backend.client.TmdbApiClient;
import com.momo.backend.client.KobisApiClient;
import com.momo.backend.dto.person.PersonDetailResponse;
import com.momo.backend.dto.person.PersonDetailResponse.FilmographyItem;
import com.momo.backend.dto.person.PersonDetailResponse.PersonInfo;
import com.momo.backend.dto.movie.MovieSearchResponse;
import com.momo.backend.entity.MovieCache;
import com.momo.backend.entity.MovieExternalIdMapping;
import com.momo.backend.repository.MovieCacheRepository;
import com.momo.backend.repository.MovieExternalIdMappingRepository;
import org.springframework.dao.DataIntegrityViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonService {

    private static final String CACHE_TYPE = "PERSON_DETAIL";
    private static final long CACHE_TTL_HOURS = 24;
    private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";

    private static final Object[] CACHE_LOCKS = createCacheLocks();

    private final TmdbApiClient tmdbApiClient;
    private final KobisApiClient kobisApiClient;
    private final MovieCacheRepository movieCacheRepository;
    private final MovieExternalIdMappingRepository movieExternalIdMappingRepository;
    private final MoviePersistenceService moviePersistenceService;
    private final MoviePosterPersistenceService moviePosterPersistenceService;
    private final ObjectMapper objectMapper;

    private static Object[] createCacheLocks() {
        Object[] locks = new Object[256];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new Object();
        }
        return locks;
    }

    public PersonDetailResponse getPersonDetail(String type, String name) {
        String normalizedType = normalizeType(type);
        String displayName = normalizeDisplayName(name);

        String cacheKey = normalizedType + "|" + normalizeCacheName(displayName);
        Object lock = CACHE_LOCKS[
                Math.floorMod(cacheKey.hashCode(), CACHE_LOCKS.length)
                ];

        synchronized (lock) {
            PersonDetailResponse cached = findValidCache(cacheKey);

            if (cached != null) {
                log.info("인물 API 캐시 HIT. type={}, key={}",
                        normalizedType, cacheKey);
                return cached;
            }

            log.info("인물 API 캐시 MISS. type={}, key={}",
                    normalizedType, cacheKey);

            PersonDetailResponse response =
                    fetchPersonDetail(normalizedType, displayName);

            saveCache(cacheKey, response);
            return response;
        }
    }

    // 영화 상세 화면에서 전달한 TMDb 인물 ID를 우선 사용해 이름 검색 오연결을 방지한다.
    public PersonDetailResponse getPersonDetailById(String type, long personId) {
        String normalizedType = normalizeType(type);
        if (personId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바르지 않은 TMDb 인물 ID입니다.");
        }
        try {
            String detailJson = tmdbApiClient.getPersonDetail(personId, "ko-KR");
            if (detailJson == null || detailJson.isBlank()) {
                throw new IllegalStateException("TMDb 인물 상세 응답이 비어 있습니다.");
            }
            JsonNode detail = objectMapper.readTree(detailJson);
            if (detail.path("id").asLong(0) != personId) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "요청한 TMDb 인물 ID와 응답이 일치하지 않습니다.");
            }
            List<FilmographyItem> filmography = extractFilmography(detail, normalizedType);
            PersonInfo personInfo = new PersonInfo(
                    personId,
                    text(detail, "name"),
                    imageUrl(text(detail, "profile_path")),
                    nullableText(detail, "birthday"),
                    nullableText(detail, "deathday"),
                    nullableText(detail, "place_of_birth"),
                    nullableText(detail, "biography"),
                    nullableText(detail, "known_for_department")
            );
            return new PersonDetailResponse(personInfo, filmography);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("TMDb 인물 ID 조회 실패. type={}, personId={}", normalizedType, personId, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "TMDb 인물 ID 조회 중 외부 API 오류가 발생했습니다.", e);
        }
    }

    /**
     * TMDb 필모그래피 ID를 KOBIS의 고유 영화 코드로 연결한다.
     * 현지화 제목/원제와 개봉 연도를 비교하고, 동명 작품은 감독으로 추가 판별한다.
     */
    public String resolveFilmographyMovie(long tmdbId) {
        if (tmdbId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바르지 않은 TMDb 영화 ID입니다.");
        }

        // 이미 검증해 저장한 TMDb-KOBIS 연결은 외부 API를 다시 호출하지 않고 재사용한다.
        String externalId = String.valueOf(tmdbId);
        var savedMapping = movieExternalIdMappingRepository
                .findByProviderAndExternalId("TMDB", externalId);
        if (savedMapping.isPresent()) {
            return savedMapping.get().getMovieCode();
        }

        try {
            JsonNode detail = objectMapper.readTree(tmdbApiClient.getMovieDetail(tmdbId));
            String localizedTitle = text(detail, "title");
            String originalTitle = text(detail, "original_title");
            String releaseYear = findKoreanReleaseYear(detail);
            if (releaseYear.isBlank()) {
                String releaseDate = text(detail, "release_date");
                releaseYear = releaseDate.length() >= 4 ? releaseDate.substring(0, 4) : "";
            }

            if (localizedTitle.isBlank() && originalTitle.isBlank()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "TMDb 영화의 제목 정보가 없어 KOBIS 작품을 확인할 수 없습니다.");
            }

            List<String> tmdbDirectors = extractDirectorNames(detail);
            List<String> searchTitles = findFilmographySearchTitles(
                    tmdbId, localizedTitle, originalTitle);

            Map<String, ScoredFilmographyCandidate> matched = new HashMap<>();
            for (String searchTitle : searchTitles) {
                collectFilmographyCandidates(
                        searchTitle, searchTitles,
                        releaseYear, tmdbDirectors, matched
                );
            }

            // 제목·연도·감독 점수를 합산한다. 한 항목이 누락되거나 표기가 달라도
            // 나머지 메타데이터가 충분히 일치하면 같은 작품으로 연결할 수 있다.
            List<ScoredFilmographyCandidate> rankedCandidates = matched.values().stream()
                    .filter(candidate -> candidate.score() >= 8)
                    .sorted(Comparator.comparingInt(ScoredFilmographyCandidate::score).reversed())
                    .toList();

            if (rankedCandidates.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "TMDb 작품 정보와 연결할 수 있는 KOBIS 영화 후보를 찾지 못했습니다.");
            }

            ScoredFilmographyCandidate bestCandidate = rankedCandidates.get(0);
            if (rankedCandidates.size() > 1
                    && bestCandidate.score() - rankedCandidates.get(1).score() < 2) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "필모그래피 작품과 유사한 KOBIS 영화가 여러 편입니다. 후보를 구분할 정보가 더 필요합니다.");
            }

            JsonNode kobisMovie = bestCandidate.movie();
            String movieCode = text(kobisMovie, "movieCd");
            log.info("필모그래피 영화 연결 완료. tmdbId={}, movieCode={}, title={}, score={}",
                    tmdbId, movieCode, text(kobisMovie, "movieNm"), bestCandidate.score());

            // KOBIS 검색 결과를 영화 테이블에 먼저 저장해 상세 페이지와 포스터가 같은 코드를 사용하게 한다.
            String kobisTitle = text(kobisMovie, "movieNm");
            String kobisReleaseDate = text(kobisMovie, "openDt");
            String kobisGenre = text(kobisMovie, "genreAlt");
            String kobisDirector = extractKobisDirectorText(kobisMovie);

            if (!movieCode.matches("[A-Za-z0-9]{8}") || kobisTitle.isBlank()) {
                throw new IllegalStateException("KOBIS 영화 검색 결과의 식별자 또는 제목이 올바르지 않습니다.");
            }

            // 영화 데이터 저장이 성공한 경우에만 외부 ID 매핑을 확정한다.
            // 저장 실패를 무시하면 매핑만 남거나 매 요청마다 재매칭하는 상태가 될 수 있다.
            moviePersistenceService.saveSearchMovies(List.of(
                    new MovieSearchResponse(
                            movieCode,
                            kobisTitle,
                            kobisReleaseDate,
                            kobisGenre,
                            kobisDirector
                    )
            ));

            // 제목 유사도 검색으로 검증된 연결 결과를 영구 저장한다.
            // 같은 TMDb ID를 동시에 처리한 경우 unique 제약조건에 먼저 저장된 연결을 재사용한다.
            try {
                movieExternalIdMappingRepository.saveAndFlush(
                        new MovieExternalIdMapping("TMDB", externalId, movieCode)
                );
            } catch (DataIntegrityViolationException duplicateMapping) {
                String existingMovieCode = movieExternalIdMappingRepository
                        .findByProviderAndExternalId("TMDB", externalId)
                        .map(MovieExternalIdMapping::getMovieCode)
                        .orElse(null);
                if (existingMovieCode != null) {
                    return existingMovieCode;
                }
                throw duplicateMapping;
            }

            // 필모그래피 카드가 사용한 TMDb poster_path를 로컬 포스터 파일로 저장한다.
            String tmdbPosterUrl = findTmdbPosterUrl(detail);
            if (!tmdbPosterUrl.isBlank()) {
                try {
                    moviePosterPersistenceService.saveFilmographyPosterIfMissing(
                            movieCode, tmdbPosterUrl);
                } catch (Exception posterError) {
                    // 포스터 저장 실패가 영화 상세 페이지 연결까지 막아서는 안 된다.
                    log.warn("필모그래피 TMDb 포스터 저장 실패. movieCode={}",
                            movieCode, posterError);
                }
            }

            return movieCode;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("TMDb 필모그래피 영화 연결 실패. tmdbId={}", tmdbId, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "필모그래피 영화 정보를 확인하는 중 외부 API 오류가 발생했습니다.", e);
        }
    }

    private String extractKobisDirectorText(JsonNode candidate) {
        JsonNode directors = candidate.path("directors");
        if (!directors.isArray()) {
            return "";
        }

        List<String> names = new ArrayList<>();
        for (JsonNode director : directors) {
            String name = text(director, "peopleNm");
            if (!name.isBlank() && !names.contains(name)) {
                names.add(name);
            }
        }
        return String.join(",", names);
    }

    private String findTmdbPosterUrl(JsonNode detail) {
        String posterPath = text(detail, "poster_path");
        if (posterPath.isBlank()
                || !posterPath.startsWith("/")
                || posterPath.contains("..")
                || posterPath.contains("?")
                || posterPath.contains("#")) {
            return "";
        }
        return IMAGE_BASE_URL + posterPath;
    }

    // TMDb 기본 제목에 없는 한국 개봉명·배급사 표기까지 KOBIS 검색어에 포함한다.
    private List<String> findFilmographySearchTitles(
            long tmdbId,
            String localizedTitle,
            String originalTitle
    ) {
        LinkedHashSet<String> titles = new LinkedHashSet<>();
        if (!localizedTitle.isBlank()) titles.add(localizedTitle.trim());
        if (!originalTitle.isBlank()) titles.add(originalTitle.trim());

        try {
            JsonNode alternatives = objectMapper.readTree(
                    tmdbApiClient.getMovieAlternativeTitles(tmdbId)
            ).path("titles");

            if (alternatives.isArray()) {
                for (JsonNode alternative : alternatives) {
                    if (!"KR".equalsIgnoreCase(text(alternative, "iso_3166_1"))) {
                        continue;
                    }

                    String title = text(alternative, "title");
                    if (!title.isBlank()) titles.add(title.trim());

                    // 과도한 외부 API 호출을 막되, 한국 대체 제목은 여러 표기를 확인한다.
                    if (titles.size() >= 6) break;
                }
            }
        } catch (Exception exception) {
            // 대체 제목 API가 실패해도 기본 제목과 원제로 연결을 계속 시도한다.
            log.debug("TMDb 한국 대체 제목 조회 실패. tmdbId={}", tmdbId, exception);
        }

        return new ArrayList<>(titles);
    }

    // TMDb의 KR 극장 개봉일을 우선 사용한다.
    // 영화제·시사회 등 최초 공개일이 극장 개봉연도를 잘못 덮지 않도록 release type을 고려한다.
    private String findKoreanReleaseYear(JsonNode detail) {
        JsonNode countries = detail.path("release_dates").path("results");
        if (!countries.isArray()) return "";

        String selectedYear = "";
        int selectedPriority = -1;

        for (JsonNode country : countries) {
            if (!"KR".equalsIgnoreCase(text(country, "iso_3166_1"))) continue;

            JsonNode releases = country.path("release_dates");
            if (!releases.isArray()) continue;

            for (JsonNode release : releases) {
                String date = text(release, "release_date");
                if (date.length() < 4 || !date.substring(0, 4).matches("\\d{4}")) {
                    continue;
                }

                int releaseType = release.path("type").asInt(0);
                int priority = switch (releaseType) {
                    case 3 -> 3; // 극장 개봉
                    case 2 -> 2; // 제한 상영
                    case 1 -> 1; // 영화제·시사회
                    default -> 0;
                };

                if (priority > selectedPriority) {
                    selectedYear = date.substring(0, 4);
                    selectedPriority = priority;
                }
            }
        }

        return selectedYear;
    }

    private List<String> extractDirectorNames(JsonNode detail) {
        List<String> names = new ArrayList<>();
        JsonNode crew = detail.path("credits").path("crew");
        if (!crew.isArray()) return names;

        for (JsonNode member : crew) {
            if ("Director".equalsIgnoreCase(text(member, "job"))) {
                String name = normalizePersonName(text(member, "name"));
                if (!name.isBlank() && !names.contains(name)) names.add(name);
            }
        }
        return names;
    }

    // KOBIS 검색 결과의 제목·개봉연도·제작연도·감독 정보를 함께 평가한다.
    private void collectFilmographyCandidates(
            String searchTitle,
            List<String> searchTitles,
            String releaseYear,
            List<String> tmdbDirectors,
            Map<String, ScoredFilmographyCandidate> matched
    ) throws Exception {
        int maxPages = 1;

        for (int page = 1; page <= maxPages; page++) {
            JsonNode root = objectMapper.readTree(
                    kobisApiClient.searchMovies(searchTitle, page, 100)
            );
            JsonNode result = root.path("movieListResult");
            JsonNode candidates = result.path("movieList");
            if (!candidates.isArray()) return;

            int totalCount = result.path("totCnt").asInt(candidates.size());
            maxPages = Math.min(5, Math.max(1, (totalCount + 99) / 100));
            for (JsonNode candidate : candidates) {
                String movieCode = text(candidate, "movieCd");
                if (!PosterFileStorageService.isValidMovieCode(movieCode)) continue;

                int score = scoreFilmographyCandidate(
                        candidate, searchTitles, releaseYear, tmdbDirectors);
                if (score < 0) continue;

                ScoredFilmographyCandidate previous = matched.get(movieCode);
                if (previous == null || score > previous.score()) {
                    matched.put(movieCode, new ScoredFilmographyCandidate(candidate, score));
                }
            }

            // 동명·리메이크 작품이 다음 페이지에 있을 수 있으므로
            // KOBIS가 반환한 전체 페이지(최대 5페이지)를 비교한다.
            if (candidates.size() < 100) return;
        }
    }

    private int scoreFilmographyCandidate(
            JsonNode candidate,
            List<String> searchTitles,
            String releaseYear,
            List<String> tmdbDirectors
    ) {
        String koreanTitle = text(candidate, "movieNm");
        String englishTitle = text(candidate, "movieNmEn");
        double titleSimilarity = Math.max(
                findBestTitleSimilarity(searchTitles, koreanTitle),
                findBestTitleSimilarity(searchTitles, englishTitle)
        );

        int titleScore;
        if (titleSimilarity >= 0.98) {
            titleScore = 8;
        } else if (titleSimilarity >= 0.90) {
            titleScore = 7;
        } else if (titleSimilarity >= 0.82) {
            titleScore = 6;
        } else if (titleSimilarity >= 0.72) {
            titleScore = 4;
        } else if (titleSimilarity >= 0.65) {
            titleScore = 2;
        } else {
            titleScore = 0;
        }

        int yearScore = scoreFilmographyReleaseYear(releaseYear, candidate);
        boolean directorMatches = !tmdbDirectors.isEmpty()
                && matchesKobisDirector(candidate, tmdbDirectors);
        boolean hasKobisDirector = candidate.path("directors").isArray()
                && !candidate.path("directors").isEmpty();

        // 제목 표기가 달라도 감독과 연도가 동시에 맞으면 검색된 후보를 연결할 수 있다.
        if (titleScore == 0 && !(directorMatches && yearScore >= 3)) {
            return -1;
        }

        int score = titleScore + yearScore;
        if (directorMatches) {
            score += 5;
        } else if (hasKobisDirector && !tmdbDirectors.isEmpty()) {
            score -= 1;
        }

        return score;
    }

    private double findBestTitleSimilarity(List<String> aliases, String candidateTitle) {
        if (candidateTitle == null || candidateTitle.isBlank()) return 0;

        double best = 0;
        for (String alias : aliases) {
            best = Math.max(best, movieTitleSimilarity(alias, candidateTitle));
        }
        return best;
    }

    private double movieTitleSimilarity(String left, String right) {
        String normalizedLeft = normalizeMovieTitle(left);
        String normalizedRight = normalizeMovieTitle(right);
        if (normalizedLeft.isBlank() || normalizedRight.isBlank()) return 0;
        if (normalizedLeft.equals(normalizedRight)) return 1.0;

        // 부제나 배급 표기가 추가된 제목은 포함 관계로 유사도를 계산한다.
        if (normalizedLeft.length() >= 4 && normalizedRight.length() >= 4
                && (normalizedLeft.contains(normalizedRight)
                || normalizedRight.contains(normalizedLeft))) {
            return 0.90;
        }

        int maxLength = Math.max(normalizedLeft.length(), normalizedRight.length());
        int[] previous = new int[normalizedRight.length() + 1];
        int[] current = new int[normalizedRight.length() + 1];

        for (int j = 0; j <= normalizedRight.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= normalizedLeft.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= normalizedRight.length(); j++) {
                int cost = normalizedLeft.charAt(i - 1) == normalizedRight.charAt(j - 1)
                        ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost
                );
            }
            int[] temporary = previous;
            previous = current;
            current = temporary;
        }

        return 1.0 - (double) previous[normalizedRight.length()] / maxLength;
    }

    // KOBIS의 실제 개봉일(openDt)과 제작연도(prdtYear)를 함께 확인한다.
    // 개봉일이 비어 있거나 연말·연초 개봉으로 연도가 어긋나는 경우 제작연도가 보완 근거가 된다.
    private int scoreFilmographyReleaseYear(String tmdbReleaseYear, JsonNode candidate) {
        if (tmdbReleaseYear == null || !tmdbReleaseYear.matches("\\d{4}")) {
            return 0;
        }

        int openDateScore = scoreYearDifference(
                tmdbReleaseYear, extractYear(text(candidate, "openDt")), false);
        int productionYearScore = scoreYearDifference(
                tmdbReleaseYear, extractYear(text(candidate, "prdtYear")), true);

        return Math.max(openDateScore, productionYearScore);
    }

    private int scoreYearDifference(
            String expectedYear,
            String candidateYear,
            boolean productionYear
    ) {
        if (candidateYear.isBlank()) return 0;

        int difference = Math.abs(
                Integer.parseInt(expectedYear) - Integer.parseInt(candidateYear));
        if (difference == 0) return productionYear ? 3 : 4;
        if (difference == 1) return 2;
        if (difference == 2) return 1;
        if (difference <= 4) return 0;
        return productionYear ? -3 : -4;
    }

    private String extractYear(String value) {
        if (value == null || value.length() < 4) return "";
        String year = value.substring(0, 4);
        return year.matches("\\d{4}") ? year : "";
    }

    private record ScoredFilmographyCandidate(JsonNode movie, int score) {
    }

    private boolean matchesKobisDirector(JsonNode candidate, List<String> tmdbDirectors) {
        JsonNode directors = candidate.path("directors");
        if (!directors.isArray()) return false;

        for (JsonNode director : directors) {
            String kobisName = normalizePersonName(text(director, "peopleNm"));
            if (!kobisName.isBlank() && tmdbDirectors.contains(kobisName)) {
                return true;
            }
        }
        return false;
    }

    private String normalizePersonName(String name) {
        return Normalizer.normalize(name == null ? "" : name.trim(), Normalizer.Form.NFKC)
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeMovieTitle(String title) {
        return Normalizer.normalize(title, Normalizer.Form.NFKC)
                .replaceAll("[\\s\\p{Punct}\\p{IsPunctuation}]", "")
                .toLowerCase(Locale.ROOT);
    }

    private PersonDetailResponse findValidCache(String cacheKey) {
        MovieCache cache = movieCacheRepository
                .findByCacheTypeAndCacheKey(CACHE_TYPE, cacheKey)
                .orElse(null);

        if (cache == null || !cache.isValid(LocalDateTime.now())) {
            return null;
        }

        try {
            return objectMapper.readValue(
                    cache.getContentJson(),
                    PersonDetailResponse.class
            );
        } catch (Exception e) {
            log.warn("인물 API 캐시 JSON 손상. 캐시 삭제 후 외부 API 재조회. key={}",
                    cacheKey, e);
            movieCacheRepository.delete(cache);
            return null;
        }
    }

    private void saveCache(
            String cacheKey,
            PersonDetailResponse response
    ) {
        try {
            String contentJson = objectMapper.writeValueAsString(response);
            LocalDateTime now = LocalDateTime.now();

            movieCacheRepository.upsert(
                    CACHE_TYPE,
                    cacheKey,
                    contentJson,
                    now,
                    now.plusHours(CACHE_TTL_HOURS)
            );
        } catch (Exception e) {
            // 캐시 저장 실패가 정상적인 인물 조회 응답까지 실패시키면 안 된다.
            log.warn("인물 API 캐시 저장 실패. key={}", cacheKey, e);
        }
    }

    private PersonDetailResponse fetchPersonDetail(
            String type,
            String name
    ) {
        try {
            JsonNode person = findPerson(name, type);

            if (person == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 인물을 TMDb에서 찾을 수 없습니다."
                );
            }

            long personId = person.path("id").asLong(0);
            if (personId <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "유효한 TMDb 인물 ID가 없습니다."
                );
            }

            String detailJson =
                    tmdbApiClient.getPersonDetail(personId, "ko-KR");

            if (detailJson == null || detailJson.isBlank()) {
                throw new IllegalStateException(
                        "TMDb 인물 상세 응답이 비어 있습니다."
                );
            }

            JsonNode detail = objectMapper.readTree(detailJson);
            List<FilmographyItem> filmography =
                    extractFilmography(detail, type);

            // 인물 상세정보가 존재하면 필모그래피가 비어 있어도 인물 페이지를 정상 반환한다.
            // 역할별 작품이 없다는 이유로 인물 전체를 404 처리하지 않는다.

            PersonInfo personInfo = new PersonInfo(
                    detail.path("id").asLong(personId),
                    text(detail, "name"),
                    imageUrl(text(detail, "profile_path")),
                    nullableText(detail, "birthday"),
                    nullableText(detail, "deathday"),
                    nullableText(detail, "place_of_birth"),
                    nullableText(detail, "biography"),
                    nullableText(detail, "known_for_department")
            );

            return new PersonDetailResponse(personInfo, filmography);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("TMDb 인물 조회 실패. type={}, name={}", type, name, e);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "외부 인물 정보 조회 중 오류가 발생했습니다.",
                    e
            );
        }
    }

    /**
     * 검색어에 포함된 문자 체계를 기준으로 우선 검색 언어를 정하고,
     * TMDb 검색 결과가 비어 있거나 역할 분야가 맞지 않으면 보조 언어로 재검색한다.
     *
     * language는 검색어 자체의 언어를 강제하는 값이 아니라 TMDb 응답의
     * 현지화 기준이므로, 한 언어의 첫 결과를 무조건 선택하지 않고
     * 모든 후보를 모아 이름 유사도와 인물 분야를 함께 평가한다.
     */
    private JsonNode findPerson(String name, String type) throws Exception {
        // 검색어의 문자 체계에 맞는 TMDb 응답 언어와 기본 영어 응답만 사용한다.
        // TMDb 검색은 입력 문자열 자체를 기준으로 하므로 특정 국가 언어를 전부 순회하지 않는다.
        LinkedHashSet<String> languages = new LinkedHashSet<>();
        languages.add(detectPrimaryLanguage(name));
        languages.add("en-US");

        Map<Long, ScoredPersonCandidate> candidates = new HashMap<>();

        for (String language : languages) {
            String json;
            try {
                json = tmdbApiClient.searchPerson(name, language);
            } catch (Exception searchError) {
                log.warn("TMDb 인물 검색 언어 실패. language={}, name={}",
                        language, name, searchError);
                continue;
            }

            if (json == null || json.isBlank()) continue;

            JsonNode results = objectMapper.readTree(json).path("results");
            if (!results.isArray()) continue;

            for (JsonNode candidate : results) {
                long id = candidate.path("id").asLong(0);
                if (id <= 0 || !matchesPersonDepartment(candidate, type)) continue;

                int score = scorePersonName(name, candidate);
                ScoredPersonCandidate previous = candidates.get(id);
                if (previous == null || score > previous.score()) {
                    candidates.put(id, new ScoredPersonCandidate(candidate, score));
                }
            }
        }

        // 검색 결과 상위 후보의 TMDb 상세정보에서 원어 이름과 also_known_as를 확인한다.
        // 검색어와 문자 체계가 다른 별칭도 같은 인물 ID로 연결하되, 분야까지 재검증한다.
        List<ScoredPersonCandidate> ranked = candidates.values().stream()
                .sorted(Comparator.comparingInt(ScoredPersonCandidate::score).reversed())
                .limit(5)
                .toList();

        JsonNode bestPerson = null;
        int bestScore = 0;
        for (ScoredPersonCandidate candidate : ranked) {
            long personId = candidate.person().path("id").asLong(0);
            if (personId <= 0) continue;

            int score = candidate.score();
            try {
                String detailJson = tmdbApiClient.getPersonDetail(personId, "en-US");
                if (detailJson != null && !detailJson.isBlank()) {
                    JsonNode detail = objectMapper.readTree(detailJson);
                    if (!matchesPersonDepartment(detail, type)) continue;

                    score = Math.max(score, scorePersonName(name, detail));
                    JsonNode aliases = detail.path("also_known_as");
                    if (aliases.isArray()) {
                        for (JsonNode alias : aliases) {
                            score = Math.max(score, scoreNameVariant(name, alias.asString("")));
                        }
                    }
                    score = Math.max(score, scoreNameVariant(name, text(detail, "original_name")));
                }
            } catch (Exception detailError) {
                log.debug("TMDb 인물 별칭 검증 실패. personId={}", personId, detailError);
            }

            if (score > bestScore) {
                bestScore = score;
                bestPerson = candidate.person();
            }
        }

        return bestScore >= 55 ? bestPerson : null;
    }

    private int scoreNameVariant(String requestedName, String candidateName) {
        String requested = normalizeSearchName(requestedName);
        String candidate = normalizeSearchName(candidateName);
        if (requested.isBlank() || candidate.isBlank()) return 0;
        if (requested.equals(candidate)) return 100;

        int maxLength = Math.max(requested.length(), candidate.length());
        if (maxLength == 0) return 0;
        double similarity = 1.0 - (double) levenshteinDistance(requested, candidate) / maxLength;
        return (int) Math.round(similarity * 80);
    }

    private String detectPrimaryLanguage(String name) {
        if (name.matches(".*[\\uAC00-\\uD7A3].*")) return "ko-KR";
        if (name.matches(".*[\\u3040-\\u30FF].*")) return "ja-JP";
        if (name.matches(".*[\\u4E00-\\u9FFF].*")) return "zh-CN";
        if (name.matches(".*[\\u0400-\\u04FF].*")) return "ru-RU";
        if (name.matches(".*[\\u0600-\\u06FF].*")) return "ar-SA";
        if (name.matches(".*[\\u0900-\\u097F].*")) return "hi-IN";
        return "en-US";
    }

    private boolean matchesPersonDepartment(JsonNode candidate, String type) {
        String department = text(candidate, "known_for_department");
        if ("director".equals(type)) {
            return "Directing".equalsIgnoreCase(department)
                    || "Director".equalsIgnoreCase(department);
        }
        return "Acting".equalsIgnoreCase(department);
    }

    private int scorePersonName(String requestedName, JsonNode candidate) {
        String candidateName = text(candidate, "name");
        String normalizedRequested = normalizePersonName(requestedName);
        String normalizedCandidate = normalizePersonName(candidateName);

        if (normalizedRequested.isBlank() || normalizedCandidate.isBlank()) return 0;
        if (normalizedRequested.equals(normalizedCandidate)) return 100;

        // TMDb 검색 결과는 입력 이름과 연관된 후보이므로, 공백·악센트·문장부호
        // 차이를 정규화한 뒤 유사도를 계산한다. 이름이 전혀 다른 첫 결과는 선택하지 않는다.
        String compactRequested = normalizeSearchName(requestedName);
        String compactCandidate = normalizeSearchName(candidateName);
        if (compactRequested.equals(compactCandidate)) return 95;

        int distance = levenshteinDistance(compactRequested, compactCandidate);
        int maxLength = Math.max(compactRequested.length(), compactCandidate.length());
        if (maxLength == 0) return 0;

        double similarity = 1.0 - (double) distance / maxLength;
        return (int) Math.round(similarity * 80);
    }

    private String normalizeSearchName(String value) {
        return Normalizer.normalize(value == null ? "" : value,
                        Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[\\s\\p{Punct}\\p{IsPunctuation}]", "")
                .toLowerCase(Locale.ROOT);
    }

    private int levenshteinDistance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) previous[j] = j;

        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost);
            }
            int[] temporary = previous;
            previous = current;
            current = temporary;
        }
        return previous[right.length()];
    }

    private record ScoredPersonCandidate(JsonNode person, int score) {
    }

    private List<FilmographyItem> extractFilmography(
            JsonNode detail,
            String type
    ) {
        Map<Long, FilmographyItem> uniqueMovies = new HashMap<>();
        JsonNode credits = detail.path("combined_credits");

        if ("director".equals(type)) {
            JsonNode crew = credits.path("crew");

            if (crew.isArray()) {
                for (JsonNode credit : crew) {
                    if ("Director".equalsIgnoreCase(text(credit, "job"))) {
                        addMovie(uniqueMovies, credit, "Director");
                    }
                }
            }
        } else {
            JsonNode cast = credits.path("cast");

            if (cast.isArray()) {
                for (JsonNode credit : cast) {
                    addMovie(
                            uniqueMovies,
                            credit,
                            nullableText(credit, "character")
                    );
                }
            }
        }

        List<FilmographyItem> result =
                new ArrayList<>(uniqueMovies.values());

        result.sort(Comparator.comparing(
                item -> item == null ? null : item.releaseDate(),
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        return result;
    }

    private void addMovie(
            Map<Long, FilmographyItem> movies,
            JsonNode credit,
            String role
    ) {
        if (!"movie".equalsIgnoreCase(text(credit, "media_type"))) {
            return;
        }

        long tmdbId = credit.path("id").asLong(0);
        if (tmdbId <= 0) {
            return;
        }

        String title = text(credit, "title");
        if (title.isBlank()) {
            title = text(credit, "original_title");
        }

        if (title.isBlank()) {
            return;
        }

        String releaseDate = nullableText(credit, "release_date");

        movies.putIfAbsent(
                tmdbId,
                new FilmographyItem(
                        tmdbId,
                        title,
                        releaseDate,
                        imageUrl(text(credit, "poster_path")),
                        role
                )
        );
    }

    private String normalizeType(String type) {
        if ("director".equalsIgnoreCase(type)) {
            return "director";
        }

        if ("actor".equalsIgnoreCase(type)) {
            return "actor";
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "인물 유형은 director 또는 actor만 지원합니다."
        );
    }

    private String normalizeDisplayName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "인물 이름이 비어 있습니다."
            );
        }

        String normalized = Normalizer.normalize(
                name.trim(),
                Normalizer.Form.NFKC
        ).replaceAll("\\s+", " ");

        if (normalized.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "인물 이름은 100자 이내로 입력해야 합니다."
            );
        }

        return normalized;
    }

    private String normalizeCacheName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private String imageUrl(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        return IMAGE_BASE_URL + path;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);

        if (value.isMissingNode() || value.isNull()) {
            return "";
        }

        return value.asString("").trim();
    }

    private String nullableText(JsonNode node, String field) {
        String value = text(node, field);
        return value.isBlank() ? null : value;
    }
}