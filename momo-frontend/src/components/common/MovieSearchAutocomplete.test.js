import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import AxiosApi from "../../api/AxiosApi";
import MovieSearchAutocomplete from "./MovieSearchAutocomplete";

const mockSearchParams = new URLSearchParams();

jest.mock("react-router-dom", () => ({
  useNavigate: () => jest.fn(),
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
            posterUrl: "http://localhost:8111/posters/20260001-test.jpg",
          },
        ],
      });
  });

  afterEach(() => {
    jest.clearAllTimers();
    jest.useRealTimers();
    jest.clearAllMocks();
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
      "/posters/20260001-test.jpg",
    );
  });
});
