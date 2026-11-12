import AxiosApi from "../api/AxiosApi";

const FAVORITES_KEY_PREFIX = "momo-favorite-movies:";
const FAVORITES_SYNCED_KEY_PREFIX = "momo-favorites-server-synced:";

let syncPromise = null;
let syncPromiseUserId = null;

// 로그인한 회원의 고유 계정 키를 반환한다.
export function getCurrentFavoriteUserId() {
  const accessToken = localStorage.getItem("accessToken");
  const loginId = localStorage.getItem("username");

  if (!accessToken || !loginId) return null;
  return loginId.trim() || null;
}

function getFavoritesKey(userId) {
  return userId ? FAVORITES_KEY_PREFIX + encodeURIComponent(userId) : null;
}

function getFavoritesSyncedKey(userId) {
  return userId
    ? FAVORITES_SYNCED_KEY_PREFIX + encodeURIComponent(userId)
    : null;
}

function notifyFavoritesChanged(favorites) {
  window.dispatchEvent(
    new CustomEvent("momo-favorites-changed", { detail: favorites }),
  );
}

function saveFavoriteCache(userId, favorites) {
  const storageKey = getFavoritesKey(userId);
  if (!storageKey) return [];

  const safeFavorites = Array.isArray(favorites) ? favorites : [];
  localStorage.setItem(storageKey, JSON.stringify(safeFavorites));
  notifyFavoritesChanged(safeFavorites);
  return safeFavorites;
}

// 관심 영화 전체 조회. 화면은 서버 응답을 캐시한 목록을 즉시 표시한다.
export function getFavoriteMovies(userId = getCurrentFavoriteUserId()) {
  const storageKey = getFavoritesKey(userId);
  if (!storageKey) return [];

  try {
    const parsedFavorites = JSON.parse(localStorage.getItem(storageKey) || "[]");
    return Array.isArray(parsedFavorites) ? parsedFavorites : [];
  } catch (error) {
    console.error("관심 영화 조회 실패:", error);
    return [];
  }
}

export function isFavoriteMovie(
  movieCode,
  userId = getCurrentFavoriteUserId(),
) {
  if (!movieCode || !userId) return false;

  return getFavoriteMovies(userId).some(
    (movie) => String(movie.movieCode) === String(movieCode),
  );
}

// 구버전에서 브라우저에 저장했던 관심 영화를 최초 1회 서버로 이전한다.
// 최초 동기화가 끝난 뒤에는 서버 목록을 단일 기준으로 사용한다.
export function syncFavoriteMoviesFromServer(
  userId = getCurrentFavoriteUserId(),
) {
  if (!userId || getCurrentFavoriteUserId() !== userId) {
    return Promise.resolve([]);
  }

  if (syncPromise && syncPromiseUserId === userId) {
    return syncPromise;
  }

  const promise = (async () => {
    const localFavorites = getFavoriteMovies(userId);
    const response = await AxiosApi.favoriteMovies();

    // 조회 중 로그아웃하거나 다른 계정으로 전환되면 이전 계정의 응답을 폐기한다.
    if (getCurrentFavoriteUserId() !== userId) return [];

    let serverFavorites = Array.isArray(response.data) ? response.data : [];
    const syncedKey = getFavoritesSyncedKey(userId);

    if (!localStorage.getItem(syncedKey)) {
      const serverCodes = new Set(
        serverFavorites.map((movie) => String(movie.movieCode)),
      );

      for (const movie of localFavorites) {
        if (getCurrentFavoriteUserId() !== userId) return [];
        if (!movie?.movieCode || serverCodes.has(String(movie.movieCode))) {
          continue;
        }

        await AxiosApi.favoriteMovieAdd({
          movieCode: String(movie.movieCode),
          title: movie.title || movie.movieTitle || "제목 정보 없음",
          releaseDate: movie.releaseDate || "",
          posterUrl: movie.posterUrl || "",
        });
      }

      if (getCurrentFavoriteUserId() !== userId) return [];

      const refreshed = await AxiosApi.favoriteMovies();
      if (getCurrentFavoriteUserId() !== userId) return [];

      serverFavorites = Array.isArray(refreshed.data) ? refreshed.data : [];
      localStorage.setItem(syncedKey, "true");
    }

    // 동기화 중 로그아웃하거나 다른 계정으로 전환된 응답은 저장하지 않는다.
    if (getCurrentFavoriteUserId() !== userId) return [];
    return saveFavoriteCache(userId, serverFavorites);
  })();

  syncPromise = promise;
  syncPromiseUserId = userId;

  return promise.finally(() => {
    if (syncPromise === promise) {
      syncPromise = null;
      syncPromiseUserId = null;
    }
  });
}

// 서버에 저장한 뒤 응답 목록으로 로컬 캐시를 갱신한다.
export async function toggleFavoriteMovieOnServer(
  movie,
  userId = getCurrentFavoriteUserId(),
) {
  if (!userId || !movie?.movieCode) return [];

  await syncFavoriteMoviesFromServer(userId);

  if (getCurrentFavoriteUserId() !== userId) return [];

  const movieCode = String(movie.movieCode);
  const exists = isFavoriteMovie(movieCode, userId);

  if (exists) {
    await AxiosApi.favoriteMovieDelete(movieCode);
  } else {
    await AxiosApi.favoriteMovieAdd({
      movieCode,
      title: movie.title || movie.movieTitle || "제목 정보 없음",
      releaseDate: movie.releaseDate || "",
      posterUrl: movie.posterUrl || "",
    });
  }

  // 저장 요청 도중 계정이 변경되었으면 새 계정으로 후속 조회를 보내지 않는다.
  if (getCurrentFavoriteUserId() !== userId) return [];

  const response = await AxiosApi.favoriteMovies();
  const favorites = Array.isArray(response.data) ? response.data : [];

  if (getCurrentFavoriteUserId() !== userId) return [];
  return saveFavoriteCache(userId, favorites);
}

// 테스트 및 이전 데이터 호환을 위한 회원별 로컬 캐시 조작 함수.
// 실제 화면의 등록·해제는 toggleFavoriteMovieOnServer를 사용한다.
export function toggleFavoriteMovie(
  movie,
  userId = getCurrentFavoriteUserId(),
) {
  if (!userId || !movie || !movie.movieCode) return [];

  const favorites = getFavoriteMovies(userId);
  const exists = favorites.some(
    (item) => String(item.movieCode) === String(movie.movieCode),
  );
  const updatedFavorites = exists
    ? favorites.filter(
        (item) => String(item.movieCode) !== String(movie.movieCode),
      )
    : [...favorites, movie];

  try {
    return saveFavoriteCache(userId, updatedFavorites);
  } catch (error) {
    console.error("관심 영화 저장 실패:", error);
    return favorites;
  }
}
