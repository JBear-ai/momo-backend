import React, { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import styled from "styled-components";

import AxiosApi from "../api/AxiosApi";

// ==========================================
// 검색 페이지 디자인
// ==========================================

const SearchPage = styled.div`
  width: 100%;
  max-width: 1320px;
  margin: 0 auto;
  padding-bottom: 60px;
  color: #1c1a17;
`;

const SearchHeading = styled.div`
  margin-bottom: 32px;

  h1 {
    margin: 0 0 10px;
    font-size: 30px;
    font-weight: 800;
    letter-spacing: -0.04em;
  }

  p {
    margin: 0;
    color: #787062;
    font-size: 14px;
  }
`;

const SearchInput = styled.input`
  width: 100%;
  padding: 17px 20px;
  box-sizing: border-box;
  border: 1px solid #e2dbd0;
  border-radius: 12px;
  background: #ffffff;
  color: #1c1a17;
  font-size: 15px;
  outline: none;

  &:focus {
    border-color: #ffd43b;
    box-shadow: 0 0 0 3px rgba(255, 212, 59, 0.18);
  }

  &::placeholder {
    color: #948b7c;
  }
`;

const MovieGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 28px 18px;
  margin-top: 36px;

  @media (max-width: 1000px) {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  @media (max-width: 700px) {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 20px 12px;
  }

  @media (max-width: 430px) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
`;

const MovieCard = styled.button`
  min-width: 0;
  padding: 0;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;

  animation: appear 0.35s ease both;

  @keyframes appear {
    from {
      opacity: 0;
      transform: translateY(12px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  &:hover img {
    transform: scale(1.04);
  }

  &:focus-visible {
    outline: 2px solid #a37e1c;
    outline-offset: 4px;
  }
`;

const Poster = styled.div`
  width: 100%;
  aspect-ratio: 2 / 2.85;
  overflow: hidden;
  border-radius: 10px;
  border: 1px solid #e2dbd0;
  background: #efece4;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
    transition: transform 0.35s ease;
  }
`;

const EmptyPoster = styled.div`
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #948b7c;
  font-size: 12px;
`;

const MovieInfo = styled.div`
  padding-top: 12px;

  h3 {
    margin: 0;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    color: #1c1a17;
    font-size: 15px;
    font-weight: 750;
  }

  p {
    margin: 6px 0 0;
    color: #787062;
    font-size: 12px;
  }
`;

const SearchMessage = styled.div`
  padding: 70px 0;
  text-align: center;
  color: #787062;
  font-size: 14px;
`;

const LoadingMessage = styled.div`
  padding: 35px 0;
  text-align: center;
  color: #787062;
  font-size: 13px;
`;

const RetryButton = styled.button`
  margin-top: 12px;
  padding: 9px 18px;
  border: 1px solid #e2dbd0;
  border-radius: 8px;
  background: #ffffff;
  color: #1c1a17;
  cursor: pointer;

  &:hover {
    background: #fff5d6;
    border-color: #ffd43b;
  }
`;

const ScrollSentinel = styled.div`
  width: 100%;
  height: 1px;
`;

// ==========================================
// 영화 카드
// ==========================================

function MovieItem({
  movie,
  index,
  posterUrl,
  posterStatus,
  onClick,
  onPosterStatusChange,
}) {
  // 이미지 로딩 실패
  const handleImageError = () => {
    onPosterStatusChange(movie.movieCode, "failed");
  };

  return (
    <MovieCard
      type="button"
      style={{ animationDelay: `${(index % 20) * 30}ms` }}
      onClick={() => onClick(movie.movieCode)}
      aria-label={`${movie.title} 상세 페이지`}
    >
      <Poster>
        {posterUrl && posterStatus !== "failed" ? (
          <img
            src={posterUrl}
            alt={`${movie.title} 포스터`}
            onError={handleImageError}
          />
        ) : (
          <EmptyPoster>
            {posterStatus === "failed" ? "포스터 없음" : "포스터 준비 중"}
          </EmptyPoster>
        )}
      </Poster>

      <MovieInfo>
        <h3>{movie.title}</h3>
        <p>{movie.releaseDate || "개봉일 정보 없음"}</p>
        <p>{movie.genre || "장르 정보 없음"}</p>
      </MovieInfo>
    </MovieCard>
  );
}

// ==========================================
// 검색 페이지
// ==========================================

function Search() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const keyword = searchParams.get("keyword") || "";

  const [movies, setMovies] = useState([]);
  const [posterStatuses, setPosterStatuses] = useState({});
  const [posterUrls, setPosterUrls] = useState({});
  const [initialLoading, setInitialLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [hasNext, setHasNext] = useState(false);
  const [error, setError] = useState("");

  const keywordRef = useRef(keyword.trim());
  const pageRef = useRef(1);
  const hasNextRef = useRef(false);
  const activeControllerRef = useRef(null);
  const isFetchingRef = useRef(false);
  const sentinelRef = useRef(null);

  keywordRef.current = keyword.trim();

  // ==========================================
  // 포스터 상태 변경
  // ==========================================

  const handlePosterStatusChange = useCallback((movieCode, status) => {
    setPosterStatuses((prev) => {
      if (prev[movieCode] === status) {
        return prev;
      }

      return {
        ...prev,
        [movieCode]: status,
      };
    });

    if (status === "failed") {
      setPosterUrls((prev) => ({
        ...prev,
        [movieCode]: "",
      }));
    }
  }, []);

  // ==========================================
  // 포스터 상태에 따른 정렬
  // 포스터 성공 → 조회 중 → 포스터 없음
  // ==========================================

  const sortedMovies = [...movies].sort((a, b) => {
    const order = {
      success: 0,
      loading: 1,
      failed: 2,
    };

    const statusA = posterStatuses[a.movieCode] || "loading";
    const statusB = posterStatuses[b.movieCode] || "loading";

    return order[statusA] - order[statusB];
  });

  // ==========================================
  // 영화 검색 API
  // ==========================================

  const loadPage = useCallback(async (page) => {
    const currentKeyword = keywordRef.current;

    if (!currentKeyword || isFetchingRef.current) return;

    isFetchingRef.current = true;

    const controller = new AbortController();
    activeControllerRef.current = controller;

    if (page === 1) {
      setInitialLoading(true);
    } else {
      setLoadingMore(true);
    }

    setError("");

    try {
      const response = await AxiosApi.movieSearch(
        currentKeyword,
        page,
        20,
        controller.signal,
      );

      if (controller.signal.aborted || keywordRef.current !== currentKeyword) {
        return;
      }

      const data = response.data;
      const newMovies = Array.isArray(data.content) ? data.content : [];

      setMovies((prevMovies) => {
        if (page === 1) {
          return newMovies;
        }

        const existingCodes = new Set(
          prevMovies.map((movie) => movie.movieCode),
        );

        const uniqueMovies = newMovies.filter(
          (movie) => !existingCodes.has(movie.movieCode),
        );

        return [...prevMovies, ...uniqueMovies];
      });

      // 새 영화의 포스터 상태 초기화
      setPosterStatuses((prev) => {
        const next = { ...prev };

        newMovies.forEach((movie) => {
          if (movie.movieCode && !next[movie.movieCode]) {
            next[movie.movieCode] = "loading";
          }
        });

        return next;
      });

      setHasNext(data.hasNext === true);
      hasNextRef.current = data.hasNext === true;
      pageRef.current = page + 1;
    } catch (err) {
      if (
        err.code !== "ERR_CANCELED" &&
        err.name !== "CanceledError" &&
        !controller.signal.aborted
      ) {
        console.error("영화 검색 실패:", err);
        setError("영화 검색 중 오류가 발생했습니다.");
      }
    } finally {
      if (activeControllerRef.current === controller) {
        activeControllerRef.current = null;
        isFetchingRef.current = false;

        setInitialLoading(false);
        setLoadingMore(false);
      }
    }
  }, []);

  // ==========================================
  // 포스터 상태 일괄 조회 및 백그라운드 갱신
  // ==========================================

  useEffect(() => {
    if (movies.length === 0) return;

    const movieCodes = movies
      .map((movie) => movie.movieCode)
      .filter((code) => code);

    if (movieCodes.length === 0) return;

    let cancelled = false;
    let timerId = null;
    let attempts = 0;

    const controller = new AbortController();

    const MAX_ATTEMPTS = 24;
    const POLLING_INTERVAL = 2500;

    const checkPosterStatuses = async () => {
      if (cancelled) return;

      attempts += 1;

      try {
        const response = await AxiosApi.moviePosterStatuses(
          movieCodes,
          controller.signal,
        );

        if (cancelled || controller.signal.aborted) return;

        const statuses = Array.isArray(response.data) ? response.data : [];

        let hasPending = false;

        const nextStatuses = {};
        const nextUrls = {};

        statuses.forEach((item) => {
          const movieCode = item.movieCode;
          const status = item.status;
          const url = item.posterUrl?.trim() || "";

          if (!movieCode) return;

          // 포스터 URL 차단
          const blockedUrl = /cineq|cinestation/i.test(url);

          if (status === "SUCCESS" && url && !blockedUrl) {
            nextStatuses[movieCode] = "success";
            nextUrls[movieCode] = url;
          } else if (status === "FAILED" || blockedUrl) {
            nextStatuses[movieCode] = "failed";
            nextUrls[movieCode] = "";
          } else {
            nextStatuses[movieCode] = "loading";
            nextUrls[movieCode] = "";
            hasPending = true;
          }
        });

        // 응답에 포함되지 않은 영화도 계속 대기
        movieCodes.forEach((movieCode) => {
          if (!nextStatuses[movieCode]) {
            nextStatuses[movieCode] = "loading";
            hasPending = true;
          }
        });

        setPosterStatuses((prev) => ({
          ...prev,
          ...nextStatuses,
        }));

        setPosterUrls((prev) => ({
          ...prev,
          ...nextUrls,
        }));

        // 처리 중인 포스터가 있으면 일정 시간 후 재조회
        if (hasPending && attempts < MAX_ATTEMPTS && !cancelled) {
          timerId = setTimeout(checkPosterStatuses, POLLING_INTERVAL);
        }
      } catch (err) {
        if (
          err.code !== "ERR_CANCELED" &&
          err.name !== "CanceledError" &&
          !cancelled
        ) {
          console.error("포스터 상태 조회 실패:", err);

          if (attempts < MAX_ATTEMPTS) {
            timerId = setTimeout(checkPosterStatuses, POLLING_INTERVAL);
          }
        }
      }
    };

    checkPosterStatuses();

    return () => {
      cancelled = true;
      controller.abort();

      if (timerId) {
        clearTimeout(timerId);
      }
    };
  }, [movies]);

  // ==========================================
  // 검색어 변경
  // ==========================================

  useEffect(() => {
    const trimmedKeyword = keyword.trim();

    if (activeControllerRef.current) {
      activeControllerRef.current.abort();
      activeControllerRef.current = null;
    }

    isFetchingRef.current = false;
    pageRef.current = 1;
    hasNextRef.current = false;

    setMovies([]);
    setPosterStatuses({});
    setPosterUrls({});
    setHasNext(false);
    setError("");
    setLoadingMore(false);

    if (!trimmedKeyword) {
      setInitialLoading(false);
      return;
    }

    setInitialLoading(true);

    const timer = setTimeout(() => {
      loadPage(1);
    }, 300);

    return () => {
      clearTimeout(timer);

      if (activeControllerRef.current) {
        activeControllerRef.current.abort();
        activeControllerRef.current = null;
      }

      isFetchingRef.current = false;
    };
  }, [keyword, loadPage]);

  // ==========================================
  // 무한 스크롤
  // ==========================================

  useEffect(() => {
    const element = sentinelRef.current;

    if (!element || !hasNext || initialLoading) return;
    if (!("IntersectionObserver" in window)) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const isVisible = entries.some((entry) => entry.isIntersecting);

        if (isVisible && hasNextRef.current && !isFetchingRef.current) {
          loadPage(pageRef.current);
        }
      },
      {
        root: null,
        rootMargin: "400px",
        threshold: 0,
      },
    );

    observer.observe(element);

    return () => observer.disconnect();
  }, [hasNext, initialLoading, loadingMore, movies.length, loadPage]);

  // ==========================================
  // 검색어 입력
  // ==========================================

  const handleKeywordChange = (e) => {
    const value = e.target.value;

    setSearchParams(value ? { keyword: value } : {}, { replace: true });
  };

  // ==========================================
  // 영화 상세 페이지 이동
  // ==========================================

  const handleMovieClick = (movieCode) => {
    navigate(`/movie/${movieCode}`);
  };

  // ==========================================
  // 검색 재시도
  // ==========================================

  const handleRetry = () => {
    if (!isFetchingRef.current) {
      loadPage(pageRef.current);
    }
  };

  // ==========================================
  // 화면 렌더링
  // ==========================================

  return (
    <SearchPage>
      <SearchHeading>
        <h1>어떤 영화를 찾으세요?</h1>
        <p>기억하고 싶은 영화를 검색하고 감상을 기록해 보세요.</p>
      </SearchHeading>

      <SearchInput
        type="text"
        placeholder="영화 제목 또는 키워드를 입력하세요."
        value={keyword}
        onChange={handleKeywordChange}
      />

      {!keyword.trim() ? (
        <SearchMessage>영화 제목을 입력해 주세요.</SearchMessage>
      ) : initialLoading && movies.length === 0 ? (
        <SearchMessage>영화를 찾고 있습니다.</SearchMessage>
      ) : error && movies.length === 0 ? (
        <SearchMessage>
          {error}
          <br />
          <RetryButton type="button" onClick={() => loadPage(1)}>
            다시 시도
          </RetryButton>
        </SearchMessage>
      ) : movies.length === 0 ? (
        <SearchMessage>검색 결과가 없습니다.</SearchMessage>
      ) : (
        <>
          <MovieGrid>
            {sortedMovies.map((movie, index) => (
              <MovieItem
                key={movie.movieCode}
                movie={movie}
                index={index}
                posterUrl={posterUrls[movie.movieCode] || ""}
                posterStatus={posterStatuses[movie.movieCode] || "loading"}
                onClick={handleMovieClick}
                onPosterStatusChange={handlePosterStatusChange}
              />
            ))}
          </MovieGrid>

          <ScrollSentinel ref={sentinelRef} />

          {loadingMore && (
            <LoadingMessage>다음 영화 목록을 불러오고 있습니다.</LoadingMessage>
          )}

          {error && (
            <SearchMessage>
              {error}
              <br />
              <RetryButton type="button" onClick={handleRetry}>
                다시 시도
              </RetryButton>
            </SearchMessage>
          )}

          {!hasNext && !loadingMore && !error && (
            <LoadingMessage>모든 영화 목록을 불러왔습니다.</LoadingMessage>
          )}
        </>
      )}
    </SearchPage>
  );
}

export default Search;
