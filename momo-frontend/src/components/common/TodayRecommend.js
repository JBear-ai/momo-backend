import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../../App.css";
import AxiosApi from "../../api/AxiosApi";
import { getFavoriteMovies } from "../../utils/favorites";

const isValidMovieCode = (movieCode) => /^[0-9]{8}$/.test(String(movieCode || ""));

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
        // 평가 조회 실패 시 해당 후보의 개인 평가를 확인할 수 없으므로 추천하지 않는다.
        return "UNKNOWN";
      }
    };

    const chooseUnratedMovie = async (candidates, excludedCodes) => {
      const uniqueCandidates = candidates.filter((item, index, list) =>
        item &&
        isValidMovieCode(item.movieCode) &&
        item.title &&
        !excludedCodes.has(String(item.movieCode)) &&
        list.findIndex((candidate) => String(candidate?.movieCode) === String(item.movieCode)) === index
      );

      for (const candidate of uniqueCandidates) {
        const myRating = await getRating(candidate.movieCode);
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

        // 서버에 저장된 좋아요 영화 목록을 취향 분석의 1순위로 사용한다.
        let likedMovies = [];
        try {
          const likedResponse = await AxiosApi.myLikedMovies(controller.signal);
          likedMovies = Array.isArray(likedResponse.data) ? likedResponse.data : [];
        } catch (likedError) {
          if (likedError.code === "ERR_CANCELED") throw likedError;
          console.warn("좋아요 영화 목록 조회 실패. 관심 영화로 대체합니다.", likedError);
        }

        const excludedCodes = new Set([
          ...favoriteCodes,
          ...likedMovies.map((item) => String(item.movieCode)).filter(Boolean),
        ]);

        // 좋아요한 영화의 장르를 우선 분석하고, 없으면 관심 영화의 장르를 사용한다.
        const seedMovie = likedMovies[0] || favorites[favorites.length - 1];
        const seedLabel = likedMovies.length > 0 ? "좋아요한 영화" : "관심 영화";
        let preferredGenre = seedMovie?.genre || "";

        if (!preferredGenre && isValidMovieCode(seedMovie?.movieCode)) {
          const detail = await AxiosApi.movieDetail(
            seedMovie.movieCode,
            controller.signal,
          );
          preferredGenre = detail.data?.genre || "";
        }

        const genre = preferredGenre
          .split(",")
          .map((value) => value.trim())
          .find(Boolean);

        if (genre) {
          const genreResponse = await AxiosApi.moviesByGenre(genre, 1, controller.signal);
          const candidates = Array.isArray(genreResponse.data)
            ? genreResponse.data
            : genreResponse.data?.content || [];
          const selected = await chooseUnratedMovie(candidates, excludedCodes);

          if (selected) {
            if (active) {
              setMovie(selected);
              setRecommendationType(`${seedLabel}의 ${genre} 장르 기반`);
            }
            return;
          }
        }

        // 관심 영화가 없거나 장르 후보가 없으면 어제 박스오피스 2~10위에서 대체한다.
        const date = new Date();
        date.setDate(date.getDate() - 1);
        const targetDate = `${date.getFullYear()}${String(date.getMonth() + 1).padStart(2, "0")}${String(date.getDate()).padStart(2, "0")}`;
        const boxOfficeResponse = await AxiosApi.dailyBoxOffice(targetDate, controller.signal);
        const boxOffice = Array.isArray(boxOfficeResponse.data) ? boxOfficeResponse.data : [];
        const fallbackCandidates = boxOffice
          .filter((item) => Number(item.rank) >= 2)
          .sort((a, b) => Number(a.rank) - Number(b.rank));
        const fallback = await chooseUnratedMovie(fallbackCandidates, excludedCodes);

        if (active) {
          setMovie(fallback);
          setRecommendationType("박스오피스 인기작 기반");
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

  if (error || !movie) {
    return <div className="recommend-state">추천 영화를 찾지 못했습니다. 영화 상세 페이지에서 관심 영화를 등록해 보세요.</div>;
  }

  const releaseYear = movie.releaseDate ? String(movie.releaseDate).slice(0, 4) : "";

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
          {releaseYear ? `${releaseYear}년 개봉` : "개봉일 정보 없음"}
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
