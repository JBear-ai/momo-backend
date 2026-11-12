const FAVORITES_KEY_PREFIX = "momo-favorite-movies:";

// 로그인한 회원의 고유 계정 키를 반환한다.
// 로그인 ID는 서버에서 중복을 허용하지 않으므로 회원별 구분 키로 사용할 수 있다.
export function getCurrentFavoriteUserId() {
  const accessToken = localStorage.getItem("accessToken");
  const loginId = localStorage.getItem("username");

  if (!accessToken || !loginId) {
    return null;
  }

  return loginId.trim() || null;
}

// 회원별 관심 영화 저장 키
function getFavoritesKey(userId) {
  if (!userId) return null;

  return FAVORITES_KEY_PREFIX + encodeURIComponent(userId);
}

// 관심 영화 전체 조회
export function getFavoriteMovies(userId = getCurrentFavoriteUserId()) {
  const storageKey = getFavoritesKey(userId);

  if (!storageKey) return [];

  try {
    const favorites = localStorage.getItem(storageKey);

    if (!favorites) {
      return [];
    }

    const parsedFavorites = JSON.parse(favorites);

    return Array.isArray(parsedFavorites) ? parsedFavorites : [];
  } catch (error) {
    console.error("관심 영화 조회 실패:", error);
    return [];
  }
}

// 특정 영화의 관심 영화 등록 여부 확인
export function isFavoriteMovie(
  movieCode,
  userId = getCurrentFavoriteUserId(),
) {
  if (!movieCode || !userId) return false;

  return getFavoriteMovies(userId).some(
    (movie) => String(movie.movieCode) === String(movieCode),
  );
}

// 관심 영화 등록 및 해제
export function toggleFavoriteMovie(
  movie,
  userId = getCurrentFavoriteUserId(),
) {
  if (!userId || !movie || !movie.movieCode) {
    return [];
  }

  const storageKey = getFavoritesKey(userId);
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
    localStorage.setItem(storageKey, JSON.stringify(updatedFavorites));

    window.dispatchEvent(
      new CustomEvent("momo-favorites-changed", {
        detail: updatedFavorites,
      }),
    );

    return updatedFavorites;
  } catch (error) {
    console.error("관심 영화 저장 실패:", error);
    return favorites;
  }
}
