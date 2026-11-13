import React from "react";
import { act, render, screen } from "@testing-library/react";
import AxiosApi from "../../api/AxiosApi";
import TodayRecommend from "./TodayRecommend";

jest.mock("react-router-dom", () => ({
  useNavigate: () => jest.fn(),
}));

jest.mock("../../api/AxiosApi", () => ({
  boxOfficeRecommendations: jest.fn(),
  movieRating: jest.fn(),
  movieDetail: jest.fn(),
  moviePoster: jest.fn(),
  moviePosterStatuses: jest.fn(),
  myLikedMovies: jest.fn(),
}));

jest.mock("../../utils/favorites", () => ({
  getFavoriteMovies: () => [],
}));

describe("TodayRecommend poster recovery", () => {
  beforeEach(() => {
    jest.useFakeTimers();
    localStorage.clear();
    AxiosApi.boxOfficeRecommendations.mockResolvedValue({
      data: [
        {
          movieCode: "20260001",
          title: "추천 영화",
          releaseDate: "20260101",
          genre: "드라마",
          posterUrl: "",
        },
      ],
    });
    AxiosApi.movieRating.mockResolvedValue({ data: { myRating: null } });
    AxiosApi.movieDetail.mockResolvedValue({ data: { overview: "추천 영화 줄거리" } });
    AxiosApi.moviePoster.mockResolvedValue({ data: { posterUrl: "" } });
    AxiosApi.moviePosterStatuses.mockResolvedValue({
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

  test("requests a missing poster and refreshes the recommendation when it is ready", async () => {
    render(React.createElement(TodayRecommend));

    await act(async () => {
      for (let i = 0; i < 12; i += 1) {
        await Promise.resolve();
      }
    });

    expect(AxiosApi.moviePoster).toHaveBeenCalledWith(
      "20260001",
      expect.anything(),
    );

    await act(async () => {
      jest.advanceTimersByTime(4000);
      for (let i = 0; i < 8; i += 1) {
        await Promise.resolve();
      }
    });

    expect(AxiosApi.moviePosterStatuses).toHaveBeenCalledWith(
      ["20260001"],
      expect.any(AbortSignal),
    );
    expect(screen.getByAltText("추천 영화").getAttribute("src")).toContain(
      "/posters/20260001-test.jpg",
    );
  });

  test("does not log the access token when a recommendation request fails", async () => {
    const error = Object.assign(new Error("request failed"), {
      code: "ERR_BAD_RESPONSE",
      response: { status: 503 },
      config: { headers: { Authorization: "Bearer test-access-token" } },
    });
    AxiosApi.boxOfficeRecommendations.mockRejectedValue(error);
    const consoleError = jest.spyOn(console, "error").mockImplementation(() => {});

    try {
      render(React.createElement(TodayRecommend));

      await act(async () => {
        for (let i = 0; i < 12; i += 1) {
          await Promise.resolve();
        }
      });

      expect(consoleError).toHaveBeenCalled();
      expect(JSON.stringify(consoleError.mock.calls)).not.toContain("test-access-token");
    } finally {
      consoleError.mockRestore();
    }
  });
});
