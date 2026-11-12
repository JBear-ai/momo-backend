import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import FavoriteStar from "./components/common/FavoriteStar";
import { toggleFavoriteMovieOnServer } from "./utils/favorites";

jest.mock("./utils/favorites", () => {
  const actual = jest.requireActual("./utils/favorites");
  return {
    ...actual,
    toggleFavoriteMovieOnServer: jest.fn(),
  };
});

beforeEach(() => {
  localStorage.clear();
  jest.clearAllMocks();
});

test("로그인 회원은 서버 저장 성공 후 관심 영화 상태를 등록하고 해제한다", async () => {
  localStorage.setItem("accessToken", "test-access-token");
  localStorage.setItem("username", "test-member");

  const movie = {
    movieCode: "20261234",
    title: "테스트 영화",
  };

  toggleFavoriteMovieOnServer
    .mockResolvedValueOnce([movie])
    .mockResolvedValueOnce([]);

  render(React.createElement(FavoriteStar, { movie }));

  fireEvent.click(screen.getByRole("button", { name: "관심 영화 추가" }));

  await waitFor(() => {
    expect(
      screen.getByRole("button", { name: "관심 영화 해제" }),
    ).toBeInTheDocument();
  });

  fireEvent.click(screen.getByRole("button", { name: "관심 영화 해제" }));

  await waitFor(() => {
    expect(
      screen.getByRole("button", { name: "관심 영화 추가" }),
    ).toBeInTheDocument();
  });

  expect(toggleFavoriteMovieOnServer).toHaveBeenNthCalledWith(1, movie, "test-member");
  expect(toggleFavoriteMovieOnServer).toHaveBeenNthCalledWith(2, movie, "test-member");
});

test("로그인하지 않은 회원에게 관심 영화 버튼을 노출하지 않는다", () => {
  const movie = {
    movieCode: "20261234",
    title: "테스트 영화",
  };

  const { container } = render(React.createElement(FavoriteStar, { movie }));

  expect(container).toBeEmptyDOMElement();
  expect(localStorage.getItem("momo-favorite-movies")).toBeNull();
});
