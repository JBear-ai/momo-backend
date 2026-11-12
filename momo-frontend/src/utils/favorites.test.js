import AxiosApi from "../api/AxiosApi";
import {
  getCurrentFavoriteUserId,
  getFavoriteMovies,
  isFavoriteMovie,
  toggleFavoriteMovie,
  syncFavoriteMoviesFromServer,
  toggleFavoriteMovieOnServer,
} from "./favorites";

jest.mock("../api/AxiosApi", () => ({
  __esModule: true,
  default: {
    favoriteMovies: jest.fn(),
    favoriteMovieAdd: jest.fn(),
    favoriteMovieDelete: jest.fn(),
  },
}));

beforeEach(() => {
  localStorage.clear();
  jest.clearAllMocks();
});

describe("회원별 관심 영화", () => {
  const movie = {
    movieCode: "20261234",
    title: "테스트 영화",
    releaseDate: "20261112",
    posterUrl: "/posters/20261234.jpg",
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

  test("최초 서버 동기화 때 기존 localStorage 관심 영화를 계정에 이전한다", async () => {
    localStorage.setItem("accessToken", "token-a");
    localStorage.setItem("username", "member-a");
    toggleFavoriteMovie(movie);

    AxiosApi.favoriteMovies
      .mockResolvedValueOnce({ data: [] })
      .mockResolvedValueOnce({ data: [movie] });
    AxiosApi.favoriteMovieAdd.mockResolvedValue({ data: movie });

    const result = await syncFavoriteMoviesFromServer();

    expect(AxiosApi.favoriteMovieAdd).toHaveBeenCalledWith(movie);
    expect(result).toEqual([movie]);
    expect(getFavoriteMovies()).toEqual([movie]);
    expect(localStorage.getItem("momo-favorites-server-synced:member-a")).toBe("true");
  });

  test("최초 동기화 이후에는 서버 목록을 기준으로 로컬 캐시를 교체한다", async () => {
    localStorage.setItem("accessToken", "token-a");
    localStorage.setItem("username", "member-a");
    localStorage.setItem("momo-favorites-server-synced:member-a", "true");
    toggleFavoriteMovie({ movieCode: "20260001", title: "오래된 로컬 영화" });
    AxiosApi.favoriteMovies.mockResolvedValue({ data: [movie] });

    const result = await syncFavoriteMoviesFromServer();

    expect(AxiosApi.favoriteMovieAdd).not.toHaveBeenCalled();
    expect(result).toEqual([movie]);
    expect(getFavoriteMovies()).toEqual([movie]);
  });

  test("계정 전환 중 이전 계정의 관심 영화가 새 계정으로 이전되지 않는다", async () => {
    localStorage.setItem("accessToken", "token-a");
    localStorage.setItem("username", "member-a");
    toggleFavoriteMovie(movie);

    let resolveServerFavorites;
    AxiosApi.favoriteMovies.mockReturnValue(
      new Promise((resolve) => {
        resolveServerFavorites = resolve;
      }),
    );

    const syncPromise = syncFavoriteMoviesFromServer("member-a");

    // 서버 응답이 오기 전에 다른 회원으로 전환한다.
    localStorage.setItem("accessToken", "token-b");
    localStorage.setItem("username", "member-b");
    resolveServerFavorites({ data: [] });

    await expect(syncPromise).resolves.toEqual([]);
    expect(AxiosApi.favoriteMovieAdd).not.toHaveBeenCalled();
    expect(localStorage.getItem("momo-favorite-movies:member-b")).toBeNull();
  });

  test("관심 영화 추가와 해제는 서버 저장 후 로컬 캐시를 갱신한다", async () => {
    localStorage.setItem("accessToken", "token-a");
    localStorage.setItem("username", "member-a");
    localStorage.setItem("momo-favorites-server-synced:member-a", "true");

    AxiosApi.favoriteMovies
      .mockResolvedValueOnce({ data: [] })
      .mockResolvedValueOnce({ data: [movie] })
      .mockResolvedValueOnce({ data: [movie] })
      .mockResolvedValueOnce({ data: [] });

    AxiosApi.favoriteMovieAdd.mockResolvedValue({ data: movie });
    AxiosApi.favoriteMovieDelete.mockResolvedValue({ status: 204 });

    const added = await toggleFavoriteMovieOnServer(movie);
    expect(AxiosApi.favoriteMovieAdd).toHaveBeenCalledWith(movie);
    expect(added).toEqual([movie]);

    const removed = await toggleFavoriteMovieOnServer(movie);
    expect(AxiosApi.favoriteMovieDelete).toHaveBeenCalledWith(movie.movieCode);
    expect(removed).toEqual([]);
    expect(getFavoriteMovies()).toEqual([]);
  });
});
