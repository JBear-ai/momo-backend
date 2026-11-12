import React, { useState, useEffect } from "react";
import {
  getCurrentFavoriteUserId,
  isFavoriteMovie,
  toggleFavoriteMovie,
} from "../../utils/favorites";

function FavoriteStar({ movie }) {
  const [userId, setUserId] = useState(() => getCurrentFavoriteUserId());
  const [favorite, setFavorite] = useState(() =>
    isFavoriteMovie(movie?.movieCode),
  );

  // 로그인 상태와 관심 영화 상태가 변경되면 현재 회원 기준으로 갱신
  useEffect(() => {
    const updateFavorite = () => {
      const currentUserId = getCurrentFavoriteUserId();

      setUserId(currentUserId);
      setFavorite(
        currentUserId
          ? isFavoriteMovie(movie?.movieCode, currentUserId)
          : false,
      );
    };

    updateFavorite();

    window.addEventListener("momo-auth-changed", updateFavorite);
    window.addEventListener("momo-favorites-changed", updateFavorite);

    return () => {
      window.removeEventListener("momo-auth-changed", updateFavorite);
      window.removeEventListener("momo-favorites-changed", updateFavorite);
    };
  }, [movie?.movieCode]);

  // 비회원에게는 회원 전용 관심 영화 기능을 노출하지 않는다.
  if (!userId) return null;

  // 관심 영화 등록 및 해제
  const handleFavoriteClick = (event) => {
    event.stopPropagation();

    if (!movie?.movieCode) return;

    const updatedFavorites = toggleFavoriteMovie(movie, userId);

    setFavorite(
      updatedFavorites.some(
        (item) => String(item.movieCode) === String(movie.movieCode),
      ),
    );
  };

  return React.createElement(
    "button",
    {
      type: "button",
      className: `favorite-star ${favorite ? "is-active" : ""}`,
      onClick: handleFavoriteClick,
      "aria-label": favorite ? "관심 영화 해제" : "관심 영화 추가",
      "aria-pressed": favorite,
      title: favorite ? "관심 영화 해제" : "관심 영화 추가",
    },
    "🎬",
  );
}

export default FavoriteStar;
