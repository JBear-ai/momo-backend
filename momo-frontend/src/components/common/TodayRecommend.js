import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import AxiosApi from "../../api/AxiosApi";
import { getFavoriteMovies } from "../../utils/favorites";
import { resolvePosterUrl } from "../../utils/mediaUrl";

const isValidMovieCode = (movieCode) => /^[A-Za-z0-9]{8}$/.test(String(movieCode || ""));
const isValidReleaseDate = (releaseDate) => /^\d{8}$/.test(String(releaseDate || ""));

function TodayRecommend() {
  const [movie, setMovie] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    let posterRefreshTimer = null;

    const getRating = async (movieCode) => {
      try {
        const response = await AxiosApi.movieRating(movieCode, controller.signal);
        return response.data?.myRating ?? null;
      } catch (requestError) {
        if (requestError.code === "ERR_CANCELED") throw requestError;
        // 평가 조회 실패 시 평가 여부를 확인할 수 없으므로 후보에서 제외한다.
        return "UNKNOWN";
      }
    };

    const attachMovieOverview = async (candidate) => {
      try {
        const detail = await AxiosApi.movieDetail(
          candidate.movieCode,
          controller.signal,
        );
        return {
          ...candidate,
          overview: String(detail.data?.overview || "").trim(),
        };
      } catch (detailError) {
        if (detailError.code === "ERR_CANCELED") throw detailError;
        console.warn("추천 영화 줄거리 조회 실패:", detailError);
        return { ...candidate, overview: "" };
      }
    };

    const refreshRecommendationPoster = (movieCode, attempt = 0) => {
      if (!active || attempt >= 180) return;

      posterRefreshTimer = window.setTimeout(async () => {
        try {
          const response = await AxiosApi.moviePosterStatuses(
            [movieCode],
            controller.signal,
          );
          const status = Array.isArray(response.data)
            ? response.data.find((item) => item.movieCode === movieCode)
            : null;

          if (status?.posterUrl) {
            if (active) {
              setMovie((current) =>
                current?.movieCode === movieCode
                  ? { ...current, posterUrl: status.posterUrl }
                  : current,
              );
            }
            return;
          }
        } catch (posterError) {
          if (posterError.code === "ERR_CANCELED") return;
          console.warn("추천 영화 포스터 상태 조회 실패:", posterError);
        }

        refreshRecommendationPoster(movieCode, attempt + 1);
      }, 4000);
    };

    const completeRecommendation = async (candidate) => {
      const candidateWithOverview = await attachMovieOverview(candidate);
      if (candidateWithOverview.posterUrl) return candidateWithOverview;

      try {
        // 추천 후보는 박스오피스 조회 경로를 거치지 않으므로 포스터 조회 API를 직접 호출해 수집을 등록한다.
        const posterResponse = await AxiosApi.moviePoster(
          candidateWithOverview.movieCode,
          controller.signal,
        );
        const posterUrl = posterResponse.data?.posterUrl;
        if (posterUrl) {
          return { ...candidateWithOverview, posterUrl };
        }
      } catch (posterError) {
        if (posterError.code === "ERR_CANCELED") throw posterError;
        console.warn("추천 영화 포스터 수집 등록 실패:", posterError);
      }

      // 비동기 수집이 완료되면 추천 배너의 포스터도 자동으로 갱신한다.
      refreshRecommendationPoster(candidateWithOverview.movieCode);
      return candidateWithOverview;
    };

    const chooseUnratedMovie = async (candidates, excludedCodes, checkedCodes) => {
      const uniqueCandidates = (Array.isArray(candidates) ? candidates : [])
        .filter((item, index, list) =>
          item &&
          isValidMovieCode(item.movieCode) &&
          item.title?.trim() &&
          isValidReleaseDate(item.releaseDate) &&
          !excludedCodes.has(String(item.movieCode)) &&
          list.findIndex((candidate) =>
            String(candidate?.movieCode) === String(item.movieCode)
          ) === index
        );

      for (const candidate of uniqueCandidates) {
        const movieCode = String(candidate.movieCode);
        if (checkedCodes.has(movieCode)) continue;
        checkedCodes.add(movieCode);

        const myRating = await getRating(movieCode);
        if (myRating === null) return candidate;
      }

      return null;
    };

    async function loadRecommendation() {
      try {
        setLoading(true);
        setError(false);

        const favorites = getFavoriteMovies();
        const favoriteCodes = new Set(
          favorites.map((item) => String(item.movieCode)).filter(Boolean),
        );

        let likedMovies = [];

        // 비로그인 사용자는 개인별 좋아요 API를 호출하지 않고 관심 영화로 추천한다.
        if (localStorage.getItem("accessToken")) {
          try {
            const likedResponse = await AxiosApi.myLikedMovies(controller.signal);
            likedMovies = Array.isArray(likedResponse.data) ? likedResponse.data : [];
          } catch (likedError) {
            if (likedError.code === "ERR_CANCELED") throw likedError;
            console.warn("좋아요 영화 목록 조회 실패. 관심 영화로 대체합니다.", likedError);
          }
        }

        // 좋아요 및 관심 영화는 추천 후보에서 제외한다.
        const excludedCodes = new Set([
          ...favoriteCodes,
          ...likedMovies.map((item) => String(item.movieCode)).filter(Boolean),
        ]);
        const checkedCodes = new Set();

        // 가장 최근 좋아요 영화를 취향 분석 기준으로 사용한다.
        const seedMovie = likedMovies[0] || favorites[favorites.length - 1];
        let preferredGenre = seedMovie?.genre || "";

        if (!preferredGenre && isValidMovieCode(seedMovie?.movieCode)) {
          try {
            const detail = await AxiosApi.movieDetail(
              seedMovie.movieCode,
              controller.signal,
            );
            preferredGenre = detail.data?.genre || "";
          } catch (detailError) {
            if (detailError.code === "ERR_CANCELED") throw detailError;
            console.warn("취향 분석용 영화 상세정보 조회 실패:", detailError);
          }
        }

        const genre = preferredGenre
          .split(",")
          .map((value) => value.trim())
          .find(Boolean);

        // 장르가 일치하면서 박스오피스 진입 이력이 확인된 영화만 조회한다.
        if (genre) {
          try {
            const genreResponse = await AxiosApi.boxOfficeRecommendations(
              genre,
              controller.signal,
            );
            const selected = await chooseUnratedMovie(
              genreResponse.data,
              excludedCodes,
              checkedCodes,
            );

            if (selected) {
              const selectedWithPoster = await completeRecommendation(selected);
              if (active) setMovie(selectedWithPoster);
              return;
            }
          } catch (genreError) {
            if (genreError.code === "ERR_CANCELED") throw genreError;
            console.warn("장르 기반 추천 조회 실패. 박스오피스 전체 후보로 전환합니다.", genreError);
          }
        }

        // 장르 후보가 없거나 취향 정보가 없으면 박스오피스 이력 전체에서 대체한다.
        const fallbackResponse = await AxiosApi.boxOfficeRecommendations(
          null,
          controller.signal,
        );
        const fallback = await chooseUnratedMovie(
          fallbackResponse.data,
          excludedCodes,
          checkedCodes,
        );

        const fallbackWithPoster = fallback
          ? await completeRecommendation(fallback)
          : null;
        if (active) setMovie(fallbackWithPoster);
      } catch (requestError) {
        if (requestError.code !== "ERR_CANCELED") {
          console.error("오늘의 추천작 조회 실패:", requestError);
          if (active) setError(true);
        }
      } finally {
        if (active) setLoading(false);
      }
    }

    loadRecommendation();
    return () => {
      active = false;
      if (posterRefreshTimer) {
        window.clearTimeout(posterRefreshTimer);
      }
      controller.abort();
    };
  }, []);

  if (loading) {
    return React.createElement(
  "div",
  {"className":"recommend-state"},
  "오늘의 추천작을 불러오는 중입니다."
);
  }

  if (error) {
    return (
      React.createElement(
  "div",
  {"className":"recommend-state"},
  "추천 영화 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요."
)
    );
  }

  if (!movie) {
    return (
      React.createElement(
  "div",
  {"className":"recommend-state"},
  "현재 추천할 수 있는 영화가 없습니다."
)
    );
  }

  const releaseYear = movie.releaseDate.slice(0, 4);

  return (
    React.createElement(
  "section",
  {"className":"recommend-banner"},
  (movie.posterUrl ? (
          React.createElement(
      "img",
      {"className":"recommend-poster","src":resolvePosterUrl(movie.posterUrl),"alt":movie.title}
    )
        ) : (
          React.createElement(
      "div",
      {"className":"recommend-poster poster-placeholder"},
      "포스터 준비 중"
    )
        )),
  React.createElement(
      "div",
      {"className":"recommend-overlay"}
    ),
  React.createElement(
      "div",
      {"className":"recommend-content"},
      React.createElement(
            "span",
            {"className":"recommend-label"},
            "오늘의 추천작"
          ),
      React.createElement(
            "h2",
            {"className":"recommend-title"},
            (movie.title)
          ),
      React.createElement(
            "p",
            {"className":"recommend-info"},
            ([releaseYear && `${releaseYear}년 개봉`, movie.genre]
                        .filter(Boolean)
                        .join(" · "))
          ),
      (movie.overview && (
                React.createElement(
            "p",
            {"className":"recommend-description"},
            (movie.overview)
          )
              )),
      React.createElement(
            "button",
            {"className":"recommend-button","type":"button","onClick":() => {
                        if (isValidMovieCode(movie.movieCode)) {
                          navigate(`/movie/${encodeURIComponent(movie.movieCode)}`);
                        }
                      }},
            "영화 자세히 보기"
          )
    )
)
  );
}

export default TodayRecommend;
