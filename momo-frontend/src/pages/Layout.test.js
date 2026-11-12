import React from "react";
import { render, waitFor } from "@testing-library/react";
import Layout from "./Layout";

jest.mock("react-router-dom", () => {
  const React = require("react");

  const createLink = ({ to, children, ...props }) =>
    React.createElement("a", { href: to, ...props }, children);

  return {
    Link: createLink,
    NavLink: createLink,
    Outlet: () => null,
    useNavigate: () => jest.fn(),
    useSearchParams: () => [new URLSearchParams()],
    useLocation: () => ({ pathname: "/" }),
  };
});

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
  const { container } = render(React.createElement(Layout));

  await waitFor(() => {
    expect(
      container.querySelector('a[href="/profile/reviews"]'),
    ).not.toBeNull();
    expect(
      container.querySelector('a[href="/profile/liked-reviews"]'),
    ).not.toBeNull();
  });
});

test("비회원에게는 회원 전용 푸터 바로가기를 표시하지 않는다", () => {
  localStorage.clear();

  const { container } = render(React.createElement(Layout));

  expect(
    container.querySelector('a[href="/profile/reviews"]'),
  ).toBeNull();
  expect(
    container.querySelector('a[href="/profile/liked-reviews"]'),
  ).toBeNull();
});
