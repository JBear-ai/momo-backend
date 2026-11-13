import React, { useState, useEffect } from "react";
import {
  getCurrentFavoriteUserId,
  isFavoriteMovie,
  toggleFavoriteMovieOnServer,
} from "../../utils/favorites";

function FavoriteStar({ movie }) {
  const [userId, setUserId] = useState(() => getCurrentFavoriteUserId());
  const [favorite, setFavorite] = useState(() =>
    isFavoriteMovie(movie?.movieCode),
  );
  const [saving, setSaving] = useState(false);

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

  if (!userId) return null;

  const handleFavoriteClick = async (event) => {
    event.stopPropagation();
    if (!movie?.movieCode || saving) return;

    setSaving(true);
    try {
      const updatedFavorites = await toggleFavoriteMovieOnServer(movie, userId);
      setFavorite(
        updatedFavorites.some(
          (item) => String(item.movieCode) === String(movie.movieCode),
        ),
      );
    } catch (error) {
      console.error("관심 영화 저장 실패:", error);
      alert("관심 영화 저장에 실패했습니다. 잠시 후 다시 시도해 주세요.");
    } finally {
      setSaving(false);
    }
  };

  return React.createElement(
    "button",
    {
      type: "button",
      className: `favorite-star ${favorite ? "is-active" : ""}`,
      onClick: handleFavoriteClick,
      disabled: saving,
      "aria-label": favorite ? "관심 영화 해제" : "관심 영화 추가",
      "aria-pressed": favorite,
      "aria-busy": saving,
      title: favorite ? "관심 영화 해제" : "관심 영화 추가",
    },
    "🎬",
  );
}

export default FavoriteStar;
