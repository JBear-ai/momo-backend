import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import AxiosApi from "../../api/AxiosApi";
import MovieSearchAutocomplete from "./MovieSearchAutocomplete";

const mockSearchParams = new URLSearchParams();
const mockNavigate = jest.fn();

jest.mock("react-router-dom", () => ({
  useNavigate: () => mockNavigate,
  useSearchParams: () => [mockSearchParams],
}));

jest.mock("../../api/AxiosApi", () => ({
  movieSearch: jest.fn(),
  moviePosterStatuses: jest.fn(),
}));

describe("MovieSearchAutocomplete poster refresh", () => {
  beforeEach(() => {
    jest.useFakeTimers();
    AxiosApi.movieSearch.mockResolvedValue({
      data: {
        content: [
          {
            movieCode: "20260001",
            title: "테스트 영화",
            releaseDate: "20260101",
            genre: "드라마",
            posterUrl: "",
          },
        ],
      },
    });
    AxiosApi.moviePosterStatuses
      .mockResolvedValueOnce({
        data: [{ movieCode: "20260001", status: "PENDING", posterUrl: "" }],
      })
      .mockResolvedValueOnce({
        data: [
          {
            movieCode: "20260001",
            status: "SUCCESS",
            posterUrl: "/posters/20260001-test.jpg",
          },
        ],
      });
  });

  afterEach(() => {
    jest.clearAllTimers();
    jest.useRealTimers();
    jest.clearAllMocks();
  });

  test("moves through results with arrow keys and opens the active result with Enter", async () => {
    render(React.createElement(MovieSearchAutocomplete));

    const input = screen.getByLabelText("영화 검색어");
    fireEvent.change(input, { target: { value: "테스트" } });

    await act(async () => {
      jest.advanceTimersByTime(300);
      for (let i = 0; i < 8; i += 1) {
        await Promise.resolve();
      }
    });

    const option = screen.getByRole("option", {
      name: "테스트 영화 상세 페이지 이동",
    });

    fireEvent.keyDown(input, { key: "ArrowDown" });

    expect(input).toHaveAttribute(
      "aria-activedescendant",
      "momo-search-option-20260001",
    );
    expect(option).toHaveAttribute("aria-selected", "true");

    fireEvent.keyDown(input, { key: "Enter" });

    expect(mockNavigate).toHaveBeenCalledWith("/movie/20260001");
  });

  test("refreshes an open search result when its queued poster becomes available", async () => {
    render(React.createElement(MovieSearchAutocomplete));

    fireEvent.change(screen.getByLabelText("영화 검색어"), {
      target: { value: "테스트" },
    });

    await act(async () => {
      jest.advanceTimersByTime(300);
      for (let i = 0; i < 8; i += 1) {
        await Promise.resolve();
      }
    });

    expect(screen.getByText("테스트 영화")).toBeInTheDocument();
    expect(screen.queryByAltText("테스트 영화 포스터")).not.toBeInTheDocument();

    await act(async () => {
      jest.advanceTimersByTime(4000);
      for (let i = 0; i < 8; i += 1) {
        await Promise.resolve();
      }
    });

    expect(AxiosApi.moviePosterStatuses).toHaveBeenCalledTimes(1);

    await act(async () => {
      jest.advanceTimersByTime(8000);
      for (let i = 0; i < 8; i += 1) {
        await Promise.resolve();
      }
    });

    expect(AxiosApi.moviePosterStatuses).toHaveBeenCalledTimes(2);
    expect(screen.getByAltText("테스트 영화 포스터").getAttribute("src")).toBe(
      "http://localhost:8111/posters/20260001-test.jpg",
    );
  });
});
