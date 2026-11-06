import React, { useState, useEffect } from "react";
import { isFavoriteMovie, toggleFavoriteMovie } from "../../utils/favorites";

function FavoriteStar({ movie }) {
  const [favorite, setFavorite] = useState(() =>
    isFavoriteMovie(movie?.movieCode),
  );

  // 영화가 변경되거나 관심 영화 상태가 변경되면 상태 갱신
  useEffect(() => {
    const updateFavorite = () => {
      setFavorite(isFavoriteMovie(movie?.movieCode));
    };

    updateFavorite();

    window.addEventListener("momo-favorites-changed", updateFavorite);

    return () => {
      window.removeEventListener("momo-favorites-changed", updateFavorite);
    };
  }, [movie?.movieCode]);

  // 관심 영화 등록 및 해제
  const handleFavoriteClick = (event) => {
    event.stopPropagation();

    if (!movie?.movieCode) return;

    toggleFavoriteMovie(movie);

    setFavorite(isFavoriteMovie(movie.movieCode));
  };

  return (
    React.createElement(
  "button",
  {"type":"button","className":`favorite-star ${favorite ? "is-active" : ""}`,"onClick":handleFavoriteClick,"aria-label":favorite ? "관심 영화 해제" : "관심 영화 추가","aria-pressed":favorite,"title":favorite ? "관심 영화 해제" : "관심 영화 추가"},
  "🎬"
)
  );
}

export default FavoriteStar;
