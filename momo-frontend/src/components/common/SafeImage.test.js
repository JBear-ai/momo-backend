import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import SafeImage from "./SafeImage";

describe("SafeImage", () => {
  test("renders the requested image when a source is available", () => {
    render(
      React.createElement(SafeImage, {
        src: "/posters/movie.jpg",
        alt: "영화 포스터",
      }),
    );

    expect(screen.getByAltText("영화 포스터").getAttribute("src")).toBe(
      "/posters/movie.jpg",
    );
  });

  test("shows the fallback when the image source is missing", () => {
    render(
      React.createElement(SafeImage, {
        src: "",
        alt: "영화 포스터",
        fallbackClassName: "poster-fallback",
      }),
    );

    expect(screen.getByRole("img", { name: "영화 포스터" })).toHaveTextContent(
      "MOMO",
    );
  });

  test("notifies the parent when the image fails", () => {
    const onError = jest.fn();

    render(
      React.createElement(SafeImage, {
        src: "/posters/missing.jpg",
        alt: "영화 포스터",
        fallback: null,
        onError,
      }),
    );

    fireEvent.error(screen.getByAltText("영화 포스터"));

    expect(onError).toHaveBeenCalledTimes(1);
    expect(screen.queryByAltText("영화 포스터")).not.toBeInTheDocument();
  });

  test("replaces a broken image with the fallback", () => {
    render(
      React.createElement(SafeImage, {
        src: "/posters/missing.jpg",
        alt: "영화 포스터",
        fallbackClassName: "poster-fallback",
      }),
    );

    fireEvent.error(screen.getByAltText("영화 포스터"));

    expect(screen.getByRole("img", { name: "영화 포스터" })).toHaveTextContent(
      "MOMO",
    );
  });
});
