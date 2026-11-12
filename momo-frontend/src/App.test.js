import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import FavoriteStar from "./components/common/FavoriteStar";

beforeEach(() => {
  localStorage.clear();
});

test("로그인 회원은 관심 영화 버튼으로 목록을 등록하고 해제할 수 있다", () => {
  localStorage.setItem("accessToken", "test-access-token");
  localStorage.setItem("username", "test-member");

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
    localStorage.getItem("momo-favorite-movies:test-member"),
  );

  expect(favorites).toHaveLength(1);
  expect(favorites[0].movieCode).toBe(movie.movieCode);
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
