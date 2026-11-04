import React, { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import styled from "styled-components";

import AxiosApi from "../api/AxiosApi";
import { formatReleaseDate } from "../utils/releaseDate";

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

  animation: appear 0.3s ease both;

  @keyframes appear {
    from {
      opacity: 0;
      transform: translateY(8px);
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

function MovieItem({ movie, index, posterUrl, onClick, onImageError }) {
  return (
    <MovieCard
      type="button"
      style={{
        animationDelay: `${(index % 20) * 20}ms`,
      }}
      onClick={() => onClick(movie.movieCode)}
      aria-label={`${movie.title} 상세 페이지`}
    >
      <Poster>
        <img
          src={posterUrl}
          alt={`${movie.title} 포스터`}
          loading="lazy"
          onError={() => onImageError(movie.movieCode)}
        />
      </Poster>

      <MovieInfo>
        <h3>{movie.title}</h3>
        <p>{formatReleaseDate(movie.releaseDate)}</p>
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
  const genre = searchParams.get("genre") || "";
  const isGenreMode = Boolean(genre.trim());
  const searchValue = isGenreMode ? genre.trim() : keyword.trim();
  const [keywordInput, setKeywordInput] = useState(keyword);

  // KOBIS에서 받아온 전체 영화 후보
  const [movies, setMovies] = useState([]);

  // 포스터가 확인된 영화만 저장하는 화면 목록
  const [visibleMovies, setVisibleMovies] = useState([]);

  // 영화별 포스터 상태
  const [posterStatuses, setPosterStatuses] = useState({});

  // 영화별 포스터 주소
  const [posterUrls, setPosterUrls] = useState({});

  const [initialLoading, setInitialLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);

  const [hasNext, setHasNext] = useState(false);
  const [error, setError] = useState("");

  // ==========================================
  // 중복 요청 및 검색 상태 관리
  // ==========================================

  const searchRef = useRef({
    value: searchValue,
    isGenreMode,
  });

  const pageRef = useRef(1);
  const hasNextRef = useRef(false);

  const activeControllerRef = useRef(null);
  const isFetchingRef = useRef(false);

  const generationRef = useRef(0);

  const sentinelRef = useRef(null);

  // React 상태의 최신 값을 비동기 작업에서 참조
  const posterStatusesRef = useRef({});

  searchRef.current = {
    value: searchValue,
    isGenreMode,
  };
  posterStatusesRef.current = posterStatuses;

  // ==========================================
  // 포스터 이미지 로딩 실패 처리
  // ==========================================

  const handlePosterImageError = useCallback((movieCode) => {
    setPosterStatuses((prev) => ({
      ...prev,
      [movieCode]: "failed",
    }));

    setPosterUrls((prev) => ({
      ...prev,
      [movieCode]: "",
    }));

    // 이미지가 실제로 로딩되지 않으면 화면에서도 제거
    setVisibleMovies((prev) =>
      prev.filter((movie) => movie.movieCode !== movieCode),
    );
  }, []);

  // ==========================================
  // 영화 검색 API
  // ==========================================

  const loadPage = useCallback(
    async (page, generation = generationRef.current) => {
      const currentSearch = searchRef.current;

      if (!currentSearch.value) return;
      if (isFetchingRef.current) return;
      if (generation !== generationRef.current) return;

      if (page > 1 && !hasNextRef.current) return;

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
        const response = currentSearch.isGenreMode
          ? await AxiosApi.moviesByGenre(
              currentSearch.value,
              page,
              controller.signal,
            )
          : await AxiosApi.movieSearch(
              currentSearch.value,
              page,
              20,
              controller.signal,
            );

        if (
          controller.signal.aborted ||
          generation !== generationRef.current ||
          searchRef.current.value !== currentSearch.value ||
          searchRef.current.isGenreMode !== currentSearch.isGenreMode
        ) {
          return;
        }

        const data = response.data;

        const newMovies = currentSearch.isGenreMode
          ? Array.isArray(data)
            ? data
            : []
          : Array.isArray(data.content)
            ? data.content
            : [];

        const validMovies = newMovies.filter(
          (movie) => movie.movieCode && movie.title,
        );

        // 영화 후보 목록에 추가
        setMovies((prevMovies) => {
          const existingCodes = new Set(
            prevMovies.map((movie) => movie.movieCode),
          );

          const uniqueMovies = validMovies.filter(
            (movie) => !existingCodes.has(movie.movieCode),
          );

          if (page === 1) {
            return uniqueMovies;
          }

          return [...prevMovies, ...uniqueMovies];
        });

        // 새 영화의 포스터 상태 초기화
        setPosterStatuses((prev) => {
          const next = { ...prev };

          validMovies.forEach((movie) => {
            if (!next[movie.movieCode]) {
              next[movie.movieCode] = "loading";
            }
          });

          return next;
        });

        const nextPageAvailable = currentSearch.isGenreMode
          ? newMovies.length === 10
          : data.hasNext === true;

        setHasNext(nextPageAvailable);
        hasNextRef.current = nextPageAvailable;

        pageRef.current = page + 1;
      } catch (err) {
        if (
          err.code !== "ERR_CANCELED" &&
          err.name !== "CanceledError" &&
          !controller.signal.aborted &&
          generation === generationRef.current
        ) {
          console.error("영화 검색 실패:", err);
          setError("영화 목록을 불러오는 중 오류가 발생했습니다.");
        }
      } finally {
        if (activeControllerRef.current === controller) {
          activeControllerRef.current = null;
          isFetchingRef.current = false;

          if (generation === generationRef.current) {
            setInitialLoading(false);
            setLoadingMore(false);
          }
        }
      }
    },
    [],
  );

  // ==========================================
  // 포스터 상태 조회
  // ==========================================

  useEffect(() => {
    if (movies.length === 0) return;

    let cancelled = false;
    let timerId = null;

    const controller = new AbortController();

    const POLLING_INTERVAL = 2500;

    const checkPosterStatuses = async () => {
      if (cancelled || controller.signal.aborted) return;

      // 이미 성공 또는 실패한 영화는 다시 조회하지 않음
      const pendingMovies = movies.filter((movie) => {
        const status = posterStatusesRef.current[movie.movieCode];

        return status !== "success" && status !== "failed";
      });

      const movieCodes = pendingMovies
        .map((movie) => movie.movieCode)
        .filter(Boolean);

      if (movieCodes.length === 0) return;

      try {
        const response = await AxiosApi.moviePosterStatuses(
          movieCodes,
          controller.signal,
        );

        if (cancelled || controller.signal.aborted) return;

        const statuses = Array.isArray(response.data) ? response.data : [];

        const statusMap = new Map();

        statuses.forEach((item) => {
          if (item.movieCode) {
            statusMap.set(item.movieCode, item);
          }
        });

        const nextStatuses = {};
        const nextUrls = {};

        const newlyVisibleMovies = [];

        // 후보 영화 순서를 기준으로 처리
        pendingMovies.forEach((movie) => {
          const item = statusMap.get(movie.movieCode);

          if (!item) {
            nextStatuses[movie.movieCode] = "loading";
            return;
          }

          const status = item.status;
          const url = item.posterUrl?.trim() || "";

          const blockedUrl = /cineq|cinestation/i.test(url);

          if (status === "SUCCESS" && url && !blockedUrl) {
            nextStatuses[movie.movieCode] = "success";
            nextUrls[movie.movieCode] = url;

            newlyVisibleMovies.push(movie);
          } else if (
            status === "FAILED" ||
            blockedUrl ||
            (status === "SUCCESS" && !url)
          ) {
            nextStatuses[movie.movieCode] = "failed";
            nextUrls[movie.movieCode] = "";
          } else {
            nextStatuses[movie.movieCode] = "loading";
          }
        });

        if (cancelled) return;

        setPosterStatuses((prev) => ({
          ...prev,
          ...nextStatuses,
        }));

        setPosterUrls((prev) => ({
          ...prev,
          ...nextUrls,
        }));

        // 성공한 영화만 기존 목록 뒤에 추가
        // 이미 표시된 영화는 중복 추가하지 않음
        if (newlyVisibleMovies.length > 0) {
          setVisibleMovies((prev) => {
            const existingCodes = new Set(prev.map((movie) => movie.movieCode));

            const uniqueMovies = newlyVisibleMovies.filter(
              (movie) => !existingCodes.has(movie.movieCode),
            );

            return [...prev, ...uniqueMovies];
          });
        }

        // 아직 처리 중인 영화가 있으면 계속 조회
        const hasPending = pendingMovies.some((movie) => {
          const nextStatus =
            nextStatuses[movie.movieCode] ||
            posterStatusesRef.current[movie.movieCode];

          return nextStatus !== "success" && nextStatus !== "failed";
        });

        if (hasPending && !cancelled) {
          timerId = setTimeout(checkPosterStatuses, POLLING_INTERVAL);
        }
      } catch (err) {
        if (
          err.code !== "ERR_CANCELED" &&
          err.name !== "CanceledError" &&
          !cancelled
        ) {
          console.error("포스터 상태 조회 실패:", err);

          // 일시적인 상태 조회 오류는 다음 주기에 다시 확인
          timerId = setTimeout(checkPosterStatuses, POLLING_INTERVAL);
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
  // 검색 조건 변경
  // ==========================================

  useEffect(() => {
    // 이전 검색 요청 무효화
    generationRef.current += 1;

    const currentGeneration = generationRef.current;

    if (activeControllerRef.current) {
      activeControllerRef.current.abort();
      activeControllerRef.current = null;
    }

    isFetchingRef.current = false;

    pageRef.current = 1;
    hasNextRef.current = false;

    setMovies([]);
    setVisibleMovies([]);

    setPosterStatuses({});
    setPosterUrls({});

    setHasNext(false);
    setError("");
    setLoadingMore(false);

    if (!searchValue) {
      setInitialLoading(false);
      return;
    }

    setInitialLoading(true);

    const timer = setTimeout(() => {
      loadPage(1, currentGeneration);
    }, 300);

    return () => {
      clearTimeout(timer);

      if (activeControllerRef.current) {
        activeControllerRef.current.abort();
        activeControllerRef.current = null;
      }

      isFetchingRef.current = false;
    };
  }, [searchValue, isGenreMode, loadPage]);

  // ==========================================
  // 포스터 대기 영화 수
  // ==========================================

  const pendingCount = movies.filter((movie) => {
    const status = posterStatuses[movie.movieCode];

    return status !== "success" && status !== "failed";
  }).length;

  // ==========================================
  // 무한 스크롤
  // ==========================================

  useEffect(() => {
    const element = sentinelRef.current;

    if (!element) return;
    if (!hasNext || initialLoading) return;
    if (!("IntersectionObserver" in window)) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const isVisible = entries.some((entry) => entry.isIntersecting);

        if (!isVisible) return;
        if (!hasNextRef.current) return;
        if (isFetchingRef.current) return;

        // 포스터 작업이 지나치게 쌓이지 않도록 대기 영화 수 제한
        if (pendingCount > 8) return;

        loadPage(pageRef.current);
      },
      {
        root: null,
        rootMargin: "700px 0px",
        threshold: 0,
      },
    );

    observer.observe(element);

    return () => {
      observer.disconnect();
    };
  }, [
    hasNext,
    initialLoading,
    loadingMore,
    pendingCount,
    visibleMovies.length,
    movies.length,
    loadPage,
  ]);

  // ==========================================
  // 검색어 입력
  // ==========================================

  useEffect(() => {
    setKeywordInput(isGenreMode ? "" : keyword);
  }, [isGenreMode, keyword]);

  const handleKeywordSubmit = (e) => {
    e.preventDefault();

    const value = keywordInput.trim();
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
      const currentPage = movies.length === 0 ? 1 : pageRef.current;

      loadPage(currentPage);
    }
  };

  // ==========================================
  // 화면 렌더링
  // ==========================================

  return (
    <SearchPage>
      <SearchHeading>
        <h1>
          {isGenreMode ? `${genre.trim()} 영화` : "어떤 영화를 찾으세요?"}
        </h1>
        <p>
          {isGenreMode
            ? "선택한 장르의 영화를 살펴보세요."
            : "기억하고 싶은 영화를 검색하고 감상을 기록해 보세요."}
        </p>
      </SearchHeading>

      <form role="search" onSubmit={handleKeywordSubmit}>
        <SearchInput
          type="search"
          aria-label="영화 제목 검색"
          placeholder="영화 제목을 입력한 뒤 Enter를 눌러 주세요."
          value={keywordInput}
          onChange={(e) => setKeywordInput(e.target.value)}
        />
      </form>

      {!searchValue ? (
        <SearchMessage>영화 제목을 입력해 주세요.</SearchMessage>
      ) : initialLoading && movies.length === 0 ? (
        <SearchMessage>영화 목록을 불러오고 있습니다.</SearchMessage>
      ) : error && movies.length === 0 ? (
        <SearchMessage>
          {error}
          <br />
          <RetryButton type="button" onClick={() => loadPage(1)}>
            다시 시도
          </RetryButton>
        </SearchMessage>
      ) : movies.length === 0 ? (
        <SearchMessage>
          {isGenreMode
            ? "선택한 장르의 영화가 없습니다."
            : "검색 결과가 없습니다."}
        </SearchMessage>
      ) : (
        <>
          {visibleMovies.length > 0 ? (
            <MovieGrid>
              {visibleMovies.map((movie, index) => (
                <MovieItem
                  key={movie.movieCode}
                  movie={movie}
                  index={index}
                  posterUrl={posterUrls[movie.movieCode] || ""}
                  onClick={handleMovieClick}
                  onImageError={handlePosterImageError}
                />
              ))}
            </MovieGrid>
          ) : (
            <LoadingMessage>
              포스터가 확인된 영화를 불러오고 있습니다.
            </LoadingMessage>
          )}

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

          {!hasNext &&
            !loadingMore &&
            pendingCount === 0 &&
            visibleMovies.length === 0 && (
              <SearchMessage>
                표시할 수 있는 포스터가 있는 영화가 없습니다.
              </SearchMessage>
            )}

          {!hasNext && !loadingMore && visibleMovies.length > 0 && (
            <LoadingMessage>모든 영화 목록을 불러왔습니다.</LoadingMessage>
          )}
        </>
      )}
    </SearchPage>
  );
}

export default Search;
