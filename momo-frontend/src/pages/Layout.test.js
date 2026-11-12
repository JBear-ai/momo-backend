import React from "react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { render, screen } from "@testing-library/react";
import Layout from "./Layout";

jest.mock("../api/AxiosApi", () => ({
  __esModule: true,
  default: {
    memberMe: jest.fn().mockResolvedValue({
      data: { nickname: "테스트 회원" },
    }),
    memberLogout: jest.fn(),
  },
  clearAuthSession: jest.fn(),
}));

beforeEach(() => {
  localStorage.clear();
  localStorage.setItem("accessToken", "test-access-token");
  localStorage.setItem("username", "test-member");
});

test("로그인한 회원에게 푸터의 작성 리뷰와 좋아요 리뷰 바로가기를 제공한다", async () => {
  render(
    <MemoryRouter initialEntries={["/"]}>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<div>홈 화면</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );

  const myReviewsLink = await screen.findByRole("link", {
    name: "작성한 리뷰",
  });
  const likedReviewsLink = screen.getByRole("link", {
    name: "좋아요한 리뷰",
  });

  expect(myReviewsLink).toHaveAttribute("href", "/profile/reviews");
  expect(likedReviewsLink).toHaveAttribute("href", "/profile/liked-reviews");
});

test("비회원에게는 회원 전용 푸터 바로가기를 표시하지 않는다", () => {
  localStorage.clear();

  render(
    <MemoryRouter initialEntries={["/"]}>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<div>홈 화면</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );

  expect(
    screen.queryByRole("link", { name: "작성한 리뷰" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("link", { name: "좋아요한 리뷰" }),
  ).not.toBeInTheDocument();
});
