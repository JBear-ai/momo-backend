import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import FavoriteStar from "./components/common/FavoriteStar";

beforeEach(() => {
  localStorage.clear();
});

test("관심 영화 버튼을 누르면 저장 상태가 변경된다", () => {
  const movie = {
    movieCode: "20261234",
    title: "테스트 영화",
  };

  render(React.createElement(FavoriteStar, { movie }));

  fireEvent.click(screen.getByRole("button", { name: "관심 영화 추가" }));

  expect(
    screen.getByRole("button", { name: "관심 영화 해제" }),
  ).toBeInTheDocument();

  const favorites = JSON.parse(
    localStorage.getItem("momo-favorite-movies"),
  );

  expect(favorites).toHaveLength(1);
  expect(favorites[0].movieCode).toBe(movie.movieCode);
});
