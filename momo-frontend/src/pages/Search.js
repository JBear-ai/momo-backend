import React, { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  SearchPage,
  SearchHeading,
  SearchInput,
  MovieGrid,
  MovieCard,
  Poster,
  MovieInfo,
  SearchMessage,
  LoadingMessage,
  RetryButton,
  ScrollSentinel,
} from "../styles/legacy/Search.styles";

import AxiosApi from "../api/AxiosApi";
import FavoriteStar from "../components/common/FavoriteStar";
import { formatReleaseDate } from "../utils/releaseDate";
import { resolvePosterUrl } from "../utils/mediaUrl";

// 검색 페이지 디자인

// 검색 목록 임시 저장 (추가 11-05)
// 상세 페이지에서 뒤로 왔을 때 API 재호출 방지

const getSearchCacheKey = (value, isGenreMode) => {
  return `momo-search-${isGenreMode ? "genre" : "keyword"}-${value}`;
};

const getSearchCache = (value, isGenreMode) => {
  if (!value) return null;

  try {
    const saved = sessionStorage.getItem(getSearchCacheKey(value, isGenreMode));

    return saved ? JSON.parse(saved) : null;
  } catch {
    return null;
  }
};

const saveSearchCache = (value, isGenreMode, data) => {
  if (!value) return;

  sessionStorage.setItem(
    getSearchCacheKey(value, isGenreMode),
    JSON.stringify(data),
  );
};

// 영화 카드

function MovieItem({ movie, index, posterUrl, onClick, onImageError }) {
  // 영화 카드 키보드 접근
  const handleCardKeyDown = (event) => {
    if (event.target !== event.currentTarget) return;

    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      onClick(movie.movieCode);
    }
  };

  return (
    React.createElement(
  MovieCard,
  {"role":"button","tabIndex":0,"style":{
          animationDelay: `${(index % 20) * 20}ms`,
        },"onClick":() => onClick(movie.movieCode),"onKeyDown":handleCardKeyDown,"aria-label":`${movie.title} 상세 페이지`},
  posterUrl &&
    React.createElement(
      Poster,
      null,
      React.createElement(
        "img",
        {
          src: resolvePosterUrl(posterUrl),
          alt: `${movie.title} 포스터`,
          loading: "lazy",
          onError: () => onImageError(movie.movieCode),
        }
      ),
      React.createElement(
        FavoriteStar,
        {
          movie: {
            ...movie,
            posterUrl,
          },
        }
      )
    ),
  React.createElement(
      MovieInfo,
      null,
      React.createElement(
            "h3",
            null,
            (movie.title)
          ),
      React.createElement(
            "p",
            null,
            (formatReleaseDate(movie.releaseDate))
          ),
      React.createElement(
            "p",
            null,
            (movie.genre || "장르 정보 없음")
          )
    )
)
  );
}

// 검색 페이지

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

  // 영화별 포스터 상태
  const [posterStatuses, setPosterStatuses] = useState({});

  // 영화별 포스터 주소
  const [posterUrls, setPosterUrls] = useState({});

  const [initialLoading, setInitialLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);

  const [hasNext, setHasNext] = useState(false);
  const [error, setError] = useState("");

  // 중복 요청 및 검색 상태 관리

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

  // 포스터 이미지 로딩 실패 처리

  const handlePosterImageError = useCallback((movieCode) => {
    setPosterStatuses((prev) => ({
      ...prev,
      [movieCode]: "failed",
    }));

    setPosterUrls((prev) => ({
      ...prev,
      [movieCode]: "",
    }));

    // 포스터가 실패해도 영화 검색 결과와 상세 페이지 진입은 유지한다.
  }, []);

  // 영화 검색 API

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

  // 포스터 상태 조회

  useEffect(() => {
    if (movies.length === 0) return;

    let cancelled = false;
    let timerId = null;

    const controller = new AbortController();

    const POLLING_BASE_DELAY = 2500;
    const MAX_POLL_ATTEMPTS = 4;
    let pollAttempt = 0;

    const markRemainingPostersFailed = () => {
      const remaining = {};
      movies.forEach((movie) => {
        const status = posterStatusesRef.current[movie.movieCode];
        if (status !== "success" && status !== "failed") {
          remaining[movie.movieCode] = "failed";
        }
      });
      if (Object.keys(remaining).length > 0) {
        setPosterStatuses((previous) => ({ ...previous, ...remaining }));
      }
    };

    const scheduleNextPoll = () => {
      if (cancelled || controller.signal.aborted) return;
      if (pollAttempt >= MAX_POLL_ATTEMPTS) {
        markRemainingPostersFailed();
        return;
      }
      const delay = Math.min(
        POLLING_BASE_DELAY * (2 ** (pollAttempt - 1)),
        10000,
      );
      timerId = setTimeout(checkPosterStatuses, delay);
    };

    const checkPosterStatuses = async () => {
      if (cancelled || controller.signal.aborted) return;

      // 이미 성공 또는 실패한 영화는 다시 조회하지 않음
      const pendingMovies = movies.filter((movie) => {
        const status = posterStatusesRef.current[movie.movieCode];

        return status !== "success" && status !== "failed";
      });

      const validPendingMovies = pendingMovies.filter((movie) =>
        /^[A-Za-z0-9]{8}$/.test(String(movie.movieCode || "")),
      );
      const invalidMovies = pendingMovies.filter(
        (movie) => !/^[A-Za-z0-9]{8}$/.test(String(movie.movieCode || "")),
      );
      const movieCodes = validPendingMovies.map((movie) => movie.movieCode);

      if (movieCodes.length === 0) {
        if (invalidMovies.length > 0) {
          const invalidStatuses = {};
          invalidMovies.forEach((movie) => {
            invalidStatuses[movie.movieCode] = "failed";
          });
          setPosterStatuses((prev) => ({ ...prev, ...invalidStatuses }));
        }
        return;
      }

      if (pollAttempt >= MAX_POLL_ATTEMPTS) {
        markRemainingPostersFailed();
        return;
      }
      pollAttempt += 1;

      try {
        // 백엔드 일괄 조회 허용량(최대 20개)에 맞춰 순차 분할 요청한다.
        // 전체 검색 결과가 누적되어도 한 요청의 크기는 20개를 넘지 않는다.
        const statusMap = new Map();
        const BATCH_SIZE = 20;

        for (let start = 0; start < movieCodes.length; start += BATCH_SIZE) {
          const batch = movieCodes.slice(start, start + BATCH_SIZE);
          const response = await AxiosApi.moviePosterStatuses(
            batch,
            controller.signal,
          );

          if (cancelled || controller.signal.aborted) return;

          const statuses = Array.isArray(response.data) ? response.data : [];
          statuses.forEach((item) => {
            if (item.movieCode) {
              statusMap.set(item.movieCode, item);
            }
          });
        }

        if (cancelled || controller.signal.aborted) return;

        const nextStatuses = {};
        const nextUrls = {};

        invalidMovies.forEach((movie) => {
          nextStatuses[movie.movieCode] = "failed";
          nextUrls[movie.movieCode] = "";
        });

        // 후보 영화 순서를 기준으로 처리
        validPendingMovies.forEach((movie) => {
          const item = statusMap.get(movie.movieCode);

          if (!item) {
            // 상태가 없는 결과도 포스터 확인 실패로 종료해 검색 결과에 남기지 않는다.
            nextStatuses[movie.movieCode] = "failed";
            nextUrls[movie.movieCode] = "";
            return;
          }

          const status = item.status;
          const url = item.posterUrl?.trim() || "";

          const blockedUrl = /cineq|cinestation/i.test(url);

          if (status === "SUCCESS" && url && !blockedUrl) {
            nextStatuses[movie.movieCode] = "success";
            nextUrls[movie.movieCode] = url;

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

        // 아직 처리 중인 영화가 있으면 계속 조회
        const hasPending = validPendingMovies.some((movie) => {
          const nextStatus =
            nextStatuses[movie.movieCode] ||
            posterStatusesRef.current[movie.movieCode];

          return nextStatus !== "success" && nextStatus !== "failed";
        });

        if (hasPending && !cancelled) {
          scheduleNextPoll();
        }
      } catch (err) {
        if (
          err.code !== "ERR_CANCELED" &&
          err.name !== "CanceledError" &&
          !cancelled
        ) {
          console.error("포스터 상태 조회 실패:", err);

          // 일시적인 상태 조회 오류도 최대 재시도 횟수 안에서만 확인한다.
          scheduleNextPoll();
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

  // 검색 조건 변경

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
    setPosterStatuses({});
    setPosterUrls({});

    setHasNext(false);
    setError("");
    setLoadingMore(false);

    if (!searchValue) {
      setInitialLoading(false);
      return;
    }

    // 이전에 조회했던 목록이 있으면 API를 다시 호출하지 않고 복원 (추가 11-05)
    const cached = getSearchCache(searchValue, isGenreMode);

    if (cached) {
      setMovies(cached.movies || []);
      setPosterStatuses(cached.posterStatuses || {});
      setPosterUrls(cached.posterUrls || {});

      hasNextRef.current = cached.hasNext === true;
      pageRef.current = cached.nextPage || 1;

      setHasNext(cached.hasNext === true);
      setInitialLoading(false);

      return;
    }

    // 저장된 목록이 없을 때만 API 호출
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

  // 현재 검색 목록 임시 저장 (수정 11-05)

  useEffect(() => {
    if (!searchValue || initialLoading || movies.length === 0) {
      return;
    }

    saveSearchCache(searchValue, isGenreMode, {
      movies,
      posterStatuses,
      posterUrls,
      hasNext,
      nextPage: pageRef.current,
    });
  }, [
    searchValue,
    isGenreMode,
    movies,
    posterStatuses,
    posterUrls,
    hasNext,
    initialLoading,
  ]);

  // 포스터 대기 영화 수

  const pendingCount = movies.filter((movie) => {
    const status = posterStatuses[movie.movieCode];

    return status !== "success" && status !== "failed";
  }).length;

  // 포스터 파일이 실제로 확인된 영화만 검색 결과로 노출한다.
  // 포스터가 없거나 파일 로딩에 실패한 작품은 카드 전체를 표시하지 않는다.
  const displayMovies = movies.filter((movie) =>
    posterStatuses[movie.movieCode] === "success"
    && Boolean(posterUrls[movie.movieCode]?.trim())
  );

  // 무한 스크롤

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

        // 포스터 수집 상태와 관계없이 검색 결과 페이지를 계속 불러온다.
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
    movies.length,
    loadPage,
  ]);

  // 검색어 입력

  useEffect(() => {
    setKeywordInput(isGenreMode ? "" : keyword);
  }, [isGenreMode, keyword]);

  const handleKeywordSubmit = (e) => {
    e.preventDefault();

    const value = keywordInput.trim();
    setSearchParams(value ? { keyword: value } : {}, { replace: true });
  };

  // 영화 상세 페이지 이동

  const handleMovieClick = (movieCode) => {
    navigate(`/movie/${movieCode}`);
  };

  // 검색 재시도

  const handleRetry = () => {
    if (!isFetchingRef.current) {
      const currentPage = movies.length === 0 ? 1 : pageRef.current;

      loadPage(currentPage);
    }
  };

  // 화면 렌더링

  return (
    React.createElement(
  SearchPage,
  null,
  React.createElement(
      SearchHeading,
      null,
      React.createElement(
            "h1",
            null,
            (isGenreMode ? `${genre.trim()} 영화` : "어떤 영화를 찾으세요?")
          ),
      React.createElement(
            "p",
            null,
            (isGenreMode
                        ? "선택한 장르의 영화를 살펴보세요."
                        : "기억하고 싶은 영화를 검색하고 감상을 기록해 보세요.")
          )
    ),
  React.createElement(
      "form",
      {"role":"search","onSubmit":handleKeywordSubmit},
      React.createElement(
            SearchInput,
            {"type":"search","aria-label":"영화 제목 검색","placeholder":"영화 제목을 입력한 뒤 Enter를 눌러 주세요.","value":keywordInput,"onChange":(e) => setKeywordInput(e.target.value)}
          )
    ),
  (!searchValue ? (
          React.createElement(
      SearchMessage,
      null,
      "영화 제목을 입력해 주세요."
    )
        ) : initialLoading && movies.length === 0 ? (
          React.createElement(
      SearchMessage,
      null,
      "영화 목록을 불러오고 있습니다."
    )
        ) : error && movies.length === 0 ? (
          React.createElement(
      SearchMessage,
      null,
      (error),
      React.createElement(
            "br",
            null
          ),
      React.createElement(
            RetryButton,
            {"type":"button","onClick":() => loadPage(1)},
            "다시 시도"
          )
    )
        ) : movies.length === 0 ? (
          React.createElement(
      SearchMessage,
      null,
      (isGenreMode
                  ? "선택한 장르의 영화가 없습니다."
                  : "검색 결과가 없습니다.")
    )
        ) : (
          React.createElement(
      React.Fragment,
      null,
      React.createElement(
        MovieGrid,
        null,
        displayMovies.map((movie, index) =>
          React.createElement(
            MovieItem,
            {
              key: movie.movieCode,
              movie,
              index,
              posterUrl: posterUrls[movie.movieCode],
              onClick: handleMovieClick,
              onImageError: handlePosterImageError,
            }
          )
        )
      ),
      React.createElement(
            ScrollSentinel,
            {"ref":sentinelRef}
          ),
      (loadingMore && (
                  React.createElement(
            LoadingMessage,
            null,
            "다음 영화 목록을 불러오고 있습니다."
          )
                )),
      (error && (
                  React.createElement(
            SearchMessage,
            null,
            (error),
            React.createElement(
                    "br",
                    null
                  ),
            React.createElement(
                    RetryButton,
                    {"type":"button","onClick":handleRetry},
                    "다시 시도"
                  )
          )
                )),
      (displayMovies.length === 0 && pendingCount > 0 && (
                  React.createElement(
            SearchMessage,
            null,
            "포스터가 확인된 영화를 불러오고 있습니다."
          )
                )),
      (!hasNext && !loadingMore && movies.length > 0 && displayMovies.length === 0 && pendingCount === 0 && (
                  React.createElement(
            SearchMessage,
            null,
            "표시할 포스터가 있는 영화가 없습니다."
          )
                )),
      (!hasNext && !loadingMore && displayMovies.length > 0 && (
                  React.createElement(
            LoadingMessage,
            null,
            "현재 제공되는 영화 목록을 모두 불러왔습니다."
          )
                ))
    )
        ))
)
  );
}

export default Search;
