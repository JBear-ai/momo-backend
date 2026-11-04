import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getFavoriteMovies } from "../utils/favorites";
import FavoriteStar from "../components/common/FavoriteStar";
import "../App.css";

function FavoriteMovies() {
  const navigate = useNavigate();
  const [movies, setMovies] = useState(() => getFavoriteMovies());

  // 관심 영화 목록 갱신
  useEffect(() => {
    const updateFavorites = (event) => {
      setMovies(event.detail || getFavoriteMovies());
    };

    window.addEventListener("momo-favorites-changed", updateFavorites);

    return () => {
      window.removeEventListener("momo-favorites-changed", updateFavorites);
    };
  }, []);

  return (
    <main className="favorite-page">
      <div className="favorite-page-container">
        <header className="favorite-page-heading">
          <h1>관심 영화</h1>
          <p>마음에 드는 영화를 모아 나만의 영화 목록을 만들어 보세요.</p>
        </header>

        <div className="favorite-list-toolbar">
          <span>총 {movies.length}개의 영화</span>

          <button
            type="button"
            className="favorite-back-button"
            onClick={() => navigate("/profile")}
          >
            ← 마이페이지
          </button>
        </div>

        {movies.length === 0 ? (
          <div className="favorite-page-empty">
            <span className="favorite-empty-symbol">🎬</span>
            <h2>아직 등록한 관심 영화가 없습니다.</h2>
            <p>마음에 드는 영화를 관심 영화로 등록해 보세요.</p>

            <button
              type="button"
              className="favorite-browse-button"
              onClick={() => navigate("/")}
            >
              영화 둘러보기
            </button>
          </div>
        ) : (
          <div className="favorite-page-grid">
            {movies.map((movie) => (
              <article className="favorite-page-card" key={movie.movieCode}>
                <div className="favorite-page-poster">
                  <button
                    type="button"
                    onClick={() => navigate(`/movie/${movie.movieCode}`)}
                    aria-label={`${movie.title} 상세 보기`}
                  >
                    {movie.posterUrl ? (
                      <img
                        src={movie.posterUrl}
                        alt={`${movie.title} 포스터`}
                        onError={(event) => {
                          event.currentTarget.style.display = "none";
                        }}
                      />
                    ) : (
                      <div className="favorite-page-no-poster">포스터 없음</div>
                    )}
                  </button>

                  <FavoriteStar movie={movie} />
                </div>

                <div className="favorite-page-movie-info">
                  <h3>{movie.title}</h3>
                  <p>{movie.releaseDate || "개봉일 정보 없음"}</p>
                </div>
              </article>
            ))}
          </div>
        )}
      </div>
    </main>
  );
}

export default FavoriteMovies;
