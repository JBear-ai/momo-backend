import {
  getCurrentFavoriteUserId,
  getFavoriteMovies,
  isFavoriteMovie,
  toggleFavoriteMovie,
} from "./favorites";

beforeEach(() => {
  localStorage.clear();
});

describe("회원별 관심 영화", () => {
  const movie = {
    movieCode: "20261234",
    title: "테스트 영화",
  };

  test("로그인하지 않은 상태에서는 관심 영화를 조회하거나 저장하지 않는다", () => {
    localStorage.setItem("momo-favorite-movies", JSON.stringify([movie]));

    expect(getCurrentFavoriteUserId()).toBeNull();
    expect(getFavoriteMovies()).toEqual([]);
    expect(isFavoriteMovie(movie.movieCode)).toBe(false);
    expect(toggleFavoriteMovie(movie)).toEqual([]);
    expect(localStorage.getItem("momo-favorite-movies")).toBe(
      JSON.stringify([movie]),
    );
  });

  test("서로 다른 회원은 같은 PC에서도 관심 영화 목록을 공유하지 않는다", () => {
    localStorage.setItem("accessToken", "token-a");
    localStorage.setItem("username", "member-a");

    toggleFavoriteMovie(movie);

    expect(getFavoriteMovies()).toEqual([movie]);
    expect(localStorage.getItem("momo-favorite-movies:member-a")).not.toBeNull();

    localStorage.setItem("accessToken", "token-b");
    localStorage.setItem("username", "member-b");

    expect(getFavoriteMovies()).toEqual([]);
    expect(isFavoriteMovie(movie.movieCode)).toBe(false);

    toggleFavoriteMovie({ movieCode: "20265678", title: "다른 영화" });

    expect(getFavoriteMovies()).toEqual([
      { movieCode: "20265678", title: "다른 영화" },
    ]);

    localStorage.setItem("username", "member-a");

    expect(getFavoriteMovies()).toEqual([movie]);
  });

  test("로그아웃하면 기존 회원의 관심 영화가 비회원에게 노출되지 않는다", () => {
    localStorage.setItem("accessToken", "token-a");
    localStorage.setItem("username", "member-a");
    toggleFavoriteMovie(movie);

    localStorage.removeItem("accessToken");
    localStorage.removeItem("username");

    expect(getFavoriteMovies()).toEqual([]);
    expect(isFavoriteMovie(movie.movieCode)).toBe(false);

    localStorage.setItem("accessToken", "token-a");
    localStorage.setItem("username", "member-a");

    expect(getFavoriteMovies()).toEqual([movie]);
  });
});
