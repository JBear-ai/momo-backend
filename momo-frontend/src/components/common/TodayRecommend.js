import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../../App.css";
import AxiosApi from "../../api/AxiosApi";
import { getFavoriteMovies } from "../../utils/favorites";

const isValidMovieCode = (movieCode) => /^\d{8}$/.test(String(movieCode || ""));
const isValidReleaseDate = (releaseDate) => /^\d{8}$/.test(String(releaseDate || ""));

function TodayRecommend() {
  const [movie, setMovie] = useState(null);
  const [recommendationType, setRecommendationType] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

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
        try {
          const likedResponse = await AxiosApi.myLikedMovies(controller.signal);
          likedMovies = Array.isArray(likedResponse.data) ? likedResponse.data : [];
        } catch (likedError) {
          if (likedError.code === "ERR_CANCELED") throw likedError;
          console.warn("좋아요 영화 목록 조회 실패. 관심 영화로 대체합니다.", likedError);
        }

        // 좋아요 및 관심 영화는 추천 후보에서 제외한다.
        const excludedCodes = new Set([
          ...favoriteCodes,
          ...likedMovies.map((item) => String(item.movieCode)).filter(Boolean),
        ]);
        const checkedCodes = new Set();

        // 가장 최근 좋아요 영화를 취향 분석 기준으로 사용한다.
        const seedMovie = likedMovies[0] || favorites[favorites.length - 1];
        const seedLabel = likedMovies.length > 0 ? "좋아요한 영화" : "관심 영화";
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
              if (active) {
                setMovie(selected);
                setRecommendationType(
                  `${seedLabel}의 ${genre} 장르 및 박스오피스 진입 이력 기반`,
                );
              }
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

        if (active) {
          setMovie(fallback);
          setRecommendationType(
            genre
              ? `${seedLabel}의 장르 후보 부족 · 박스오피스 인기 이력 기반`
              : "박스오피스 진입 이력 기반",
          );
        }
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
      controller.abort();
    };
  }, []);

  if (loading) {
    return <div className="recommend-state">오늘의 추천작을 불러오는 중입니다.</div>;
  }

  if (error) {
    return (
      <div className="recommend-state">
        추천 영화 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.
      </div>
    );
  }

  if (!movie) {
    return (
      <div className="recommend-state">
        박스오피스 진입 이력이 확인된 영화 중 추천 조건을 충족하는 작품이 없습니다.
      </div>
    );
  }

  const releaseYear = movie.releaseDate.slice(0, 4);

  return (
    <section className="recommend-banner">
      {movie.posterUrl ? (
        <img className="recommend-poster" src={movie.posterUrl} alt={movie.title} />
      ) : (
        <div className="recommend-poster poster-placeholder">포스터 준비 중</div>
      )}
      <div className="recommend-overlay" />
      <div className="recommend-content">
        <span className="recommend-label">오늘의 추천작</span>
        <h2 className="recommend-title">{movie.title}</h2>
        <p className="recommend-info">
          {releaseYear}년 개봉
          {" · "}{recommendationType}
        </p>
        <button
          className="recommend-button"
          type="button"
          onClick={() => {
            if (isValidMovieCode(movie.movieCode)) {
              navigate(`/movie/${encodeURIComponent(movie.movieCode)}`);
            }
          }}
        >
          영화 자세히 보기
        </button>
      </div>
    </section>
  );
}

export default TodayRecommend;
