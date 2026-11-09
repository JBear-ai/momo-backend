const FAVORITES_KEY = "momo-favorite-movies";

// 관심 영화 전체 조회
export function getFavoriteMovies() {
  try {
    const favorites = localStorage.getItem(FAVORITES_KEY);

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
export function isFavoriteMovie(movieCode) {
  if (!movieCode) return false;

  return getFavoriteMovies().some(
    (movie) => String(movie.movieCode) === String(movieCode),
  );
}

// 관심 영화 등록 및 해제
export function toggleFavoriteMovie(movie) {
  if (!movie || !movie.movieCode) {
    return getFavoriteMovies();
  }

  const favorites = getFavoriteMovies();

  const exists = favorites.some(
    (item) => String(item.movieCode) === String(movie.movieCode),
  );

  const updatedFavorites = exists
    ? favorites.filter(
        (item) => String(item.movieCode) !== String(movie.movieCode),
      )
    : [...favorites, movie];

  try {
    localStorage.setItem(FAVORITES_KEY, JSON.stringify(updatedFavorites));

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
