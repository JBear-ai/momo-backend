package com.momo.backend.service;

import com.momo.backend.client.TmdbApiClient;
import com.momo.backend.client.KobisApiClient;
import com.momo.backend.dto.person.PersonDetailResponse;
import com.momo.backend.dto.person.PersonDetailResponse.FilmographyItem;
import com.momo.backend.dto.person.PersonDetailResponse.PersonInfo;
import com.momo.backend.entity.MovieCache;
import com.momo.backend.repository.MovieCacheRepository;
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

    /**
     * TMDb 필모그래피 ID를 KOBIS의 고유 영화 코드로 연결한다.
     * 현지화 제목/원제와 개봉 연도를 비교하고, 동명 작품은 감독으로 추가 판별한다.
     */
    public String resolveFilmographyMovie(long tmdbId) {
        if (tmdbId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바르지 않은 TMDb 영화 ID입니다.");
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
            List<String> searchTitles = new ArrayList<>();
            if (!localizedTitle.isBlank()) searchTitles.add(localizedTitle);
            if (!originalTitle.isBlank()
                    && searchTitles.stream().noneMatch(value -> sameMovieTitle(value, originalTitle))) {
                searchTitles.add(originalTitle);
            }

            Map<String, JsonNode> matched = new HashMap<>();
            for (String searchTitle : searchTitles) {
                collectFilmographyCandidates(
                        searchTitle, localizedTitle, originalTitle,
                        releaseYear, tmdbDirectors, matched
                );
            }

            // 동명 작품이 여러 편이면 TMDb와 KOBIS 양쪽에 있는 감독 정보로 한 번 더 좁힌다.
            boolean hasConfirmedDirectorMatch = !tmdbDirectors.isEmpty()
                    && matched.values().stream()
                            .anyMatch(candidate -> matchesKobisDirector(candidate, tmdbDirectors));

            // 감독 이름 표기가 달라 전혀 일치하지 않는 경우에는 후보를 전부 제거하지 않고 모호한 상태로 남긴다.
            if (matched.size() > 1 && hasConfirmedDirectorMatch) {
                matched.entrySet().removeIf(entry ->
                        !matchesKobisDirector(entry.getValue(), tmdbDirectors));
            }

            if (matched.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "TMDb 제목·한국 개봉연도·감독 정보를 기준으로 일치하는 KOBIS 작품을 찾을 수 없습니다.");
            }
            if (matched.size() > 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "동일 작품으로 판단되는 KOBIS 영화가 여러 편입니다. 잘못된 영화로 연결하지 않도록 자동 선택을 중단했습니다.");
            }

            return matched.keySet().iterator().next();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("TMDb 필모그래피 영화 연결 실패. tmdbId={}", tmdbId, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "필모그래피 영화 정보를 확인하는 중 외부 API 오류가 발생했습니다.", e);
        }
    }

    // TMDb의 KR 지역 개봉일을 우선 사용한다. 해외 최초 개봉연도와 국내 개봉연도가 다른 작품을 보정한다.
    private String findKoreanReleaseYear(JsonNode detail) {
        JsonNode countries = detail.path("release_dates").path("results");
        if (!countries.isArray()) return "";

        for (JsonNode country : countries) {
            if (!"KR".equalsIgnoreCase(text(country, "iso_3166_1"))) continue;

            JsonNode releases = country.path("release_dates");
            if (!releases.isArray()) continue;

            for (JsonNode release : releases) {
                String date = text(release, "release_date");
                if (date.length() >= 4 && date.substring(0, 4).matches("\\d{4}")) {
                    return date.substring(0, 4);
                }
            }
        }
        return "";
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

    // 첫 페이지에 일치 후보가 없을 때만 다음 페이지를 조회해 검색 결과 상한에 걸리는 문제를 줄인다.
    private void collectFilmographyCandidates(
            String searchTitle,
            String localizedTitle,
            String originalTitle,
            String releaseYear,
            List<String> tmdbDirectors,
            Map<String, JsonNode> matched
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
            boolean foundOnPage = false;

            for (JsonNode candidate : candidates) {
                String movieCode = text(candidate, "movieCd");
                if (movieCode.isBlank()) continue;

                String koreanTitle = text(candidate, "movieNm");
                String englishTitle = text(candidate, "movieNmEn");
                boolean titleMatches =
                        sameMovieTitle(localizedTitle, koreanTitle)
                        || sameMovieTitle(localizedTitle, englishTitle)
                        || sameMovieTitle(originalTitle, koreanTitle)
                        || sameMovieTitle(originalTitle, englishTitle);
                if (!titleMatches) continue;

                String candidateYear = text(candidate, "openDt");
                boolean yearMatches = !releaseYear.isBlank()
                        && candidateYear.length() >= 4
                        && releaseYear.equals(candidateYear.substring(0, 4));
                boolean directorMatches = !tmdbDirectors.isEmpty()
                        && matchesKobisDirector(candidate, tmdbDirectors);

                // 국내 개봉연도 또는 감독이 확인되지 않는 후보는 오매칭 방지를 위해 제외한다.
                if (!yearMatches && !directorMatches) continue;

                matched.put(movieCode, candidate);
                foundOnPage = true;
            }

            if (foundOnPage || candidates.size() < 100) return;
        }
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

    private boolean sameMovieTitle(String left, String right) {
        if (left == null || right == null || left.isBlank() || right.isBlank()) return false;
        return normalizeMovieTitle(left).equals(normalizeMovieTitle(right));
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

    private JsonNode findPerson(String name, String type) throws Exception {
        JsonNode person = findPersonByLanguage(name, type, "ko-KR");

        if (person == null) {
            person = findPersonByLanguage(name, type, "ja-JP");
        }

        return person;
    }

    private JsonNode findPersonByLanguage(
            String name,
            String type,
            String language
    ) throws Exception {
        String json = tmdbApiClient.searchPerson(name, language);

        if (json == null || json.isBlank()) {
            return null;
        }

        JsonNode results = objectMapper.readTree(json).path("results");
        if (!results.isArray()) {
            return null;
        }

        for (JsonNode candidate : results) {
            String department = text(candidate, "known_for_department");

            if ("director".equals(type)
                    && ("Directing".equalsIgnoreCase(department)
                    || "Director".equalsIgnoreCase(department))) {
                return candidate;
            }

            if ("actor".equals(type)
                    && "Acting".equalsIgnoreCase(department)) {
                return candidate;
            }
        }

        return null;
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