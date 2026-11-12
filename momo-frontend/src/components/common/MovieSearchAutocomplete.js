import React, { useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

import AxiosApi from "../../api/AxiosApi";
import SafeImage from "./SafeImage";
import { resolvePosterUrl } from "../../utils/mediaUrl";

function MovieSearchAutocomplete() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const searchAreaRef = useRef(null);
  const requestRef = useRef(null);

  const [keyword, setKeyword] = useState(searchParams.get("keyword") || "");

  const [movies, setMovies] = useState([]);
  const [isOpen, setIsOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // URL 검색어 변경 시 검색창 동기화
  useEffect(() => {
    setKeyword(searchParams.get("keyword") || "");
    setIsOpen(false);
  }, [searchParams]);

  // 영화 검색 자동완성
  useEffect(() => {
    const trimmedKeyword = keyword.trim();

    if (!isOpen || trimmedKeyword.length < 2) {
      setMovies([]);
      setLoading(false);
      setError("");
      return;
    }

    const controller = new AbortController();
    requestRef.current = controller;

    const timer = setTimeout(async () => {
      setLoading(true);
      setError("");
      setMovies([]);

      try {
        const response = await AxiosApi.movieSearch(
          trimmedKeyword,
          1,
          100,
          controller.signal,
        );

        if (controller.signal.aborted) return;

        const data = response.data;

        const results = Array.isArray(data?.content) ? data.content : [];

        const validMovies = results.filter(
          (movie) => movie.movieCode && movie.title,
        );

        setMovies(validMovies);
      } catch (err) {
        if (
          !controller.signal.aborted &&
          err.code !== "ERR_CANCELED" &&
          err.name !== "CanceledError"
        ) {
          console.error("영화 자동완성 검색 실패:", err);
          setError("영화 검색 중 오류가 발생했습니다.");
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }, 300);

    return () => {
      clearTimeout(timer);
      controller.abort();

      if (requestRef.current === controller) {
        requestRef.current = null;
      }
    };
  }, [keyword, isOpen]);

  // 검색창 외부 클릭 시 자동완성 닫기
  useEffect(() => {
    const handleOutsideClick = (event) => {
      if (
        searchAreaRef.current &&
        !searchAreaRef.current.contains(event.target)
      ) {
        setIsOpen(false);
      }
    };

    document.addEventListener("mousedown", handleOutsideClick);

    return () => {
      document.removeEventListener("mousedown", handleOutsideClick);
    };
  }, []);

  // 검색 결과 페이지 이동
  const handleSearchSubmit = (event) => {
    event.preventDefault();

    const value = keyword.trim();

    if (!value) return;

    setIsOpen(false);

    navigate(`/search?keyword=${encodeURIComponent(value)}`);
  };

  // 영화 상세 페이지 이동
  const handleMovieSelect = (movie) => {
    if (!movie?.movieCode) return;

    setIsOpen(false);

    if (requestRef.current) {
      requestRef.current.abort();
    }

    navigate(`/movie/${encodeURIComponent(movie.movieCode)}`);
  };

  // 키보드 ESC로 자동완성 닫기
  const handleKeyDown = (event) => {
    if (event.key === "Escape") {
      setIsOpen(false);
    }
  };

  // 개봉일 표시
  const formatReleaseDate = (releaseDate) => {
    if (!releaseDate) return "개봉일 정보 없음";

    return releaseDate;
  };

  return (
    <div
      className="momo-search-area"
      ref={searchAreaRef}
      onKeyDown={handleKeyDown}
    >
      <form
        className="momo-search-form"
        onSubmit={handleSearchSubmit}
        role="search"
      >
        <input
          className="momo-search-input"
          type="text"
          role="combobox"
          value={keyword}
          placeholder="영화 제목 또는 키워드 검색"
          aria-label="영화 검색어"
          aria-expanded={isOpen}
          aria-autocomplete="list"
          aria-controls="momo-search-listbox"
          aria-haspopup="listbox"
          onChange={(event) => {
            setKeyword(event.target.value);
            setIsOpen(true);
          }}
          onFocus={() => {
            if (keyword.trim().length >= 2) {
              setIsOpen(true);
            }
          }}
        />

        <button className="momo-search-button" type="submit">
          검색
        </button>
      </form>

      {isOpen && keyword.trim().length >= 2 && (
        <div
          className="momo-search-dropdown"
          id="momo-search-listbox"
          role="listbox"
        >
          {loading && (
            <div className="momo-search-message">영화를 검색하고 있습니다.</div>
          )}

          {!loading && error && (
            <div className="momo-search-message">{error}</div>
          )}

          {!loading && !error && movies.length === 0 && (
            <div className="momo-search-message">검색 결과가 없습니다.</div>
          )}

          {!loading &&
            !error &&
            movies.map((movie) => (
              <button
                type="button"
                role="option"
                aria-selected="false"
                className="momo-search-result"
                key={movie.movieCode}
                onClick={() => handleMovieSelect(movie)}
                aria-label={`${movie.title} 상세 페이지 이동`}
              >
                <div className="momo-search-poster">
                  <SafeImage
                    src={resolvePosterUrl(movie.posterUrl)}
                    alt={`${movie.title} 포스터`}
                    loading="lazy"
                    fallbackClassName="momo-search-poster-fallback"
                    fallbackText="🎬"
                  />
                </div>

                <div className="momo-search-info">
                  <span className="momo-search-title">{movie.title}</span>

                  <span className="momo-search-meta">
                    {formatReleaseDate(movie.releaseDate)}
                    {" · "}
                    {movie.genre || "장르 정보 없음"}
                  </span>
                </div>
              </button>
            ))}
        </div>
      )}
    </div>
  );
}

export default MovieSearchAutocomplete;
