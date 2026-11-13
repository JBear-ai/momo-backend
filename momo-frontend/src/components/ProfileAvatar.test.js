import React from "react";
import { render } from "@testing-library/react";
import AxiosApi from "../api/AxiosApi";
import ProfileAvatar from "./ProfileAvatar";

jest.mock("../api/AxiosApi", () => ({
  __esModule: true,
  default: {
    profileImageByUserIdGet: jest.fn(),
  },
}));

describe("ProfileAvatar", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("컴포넌트가 사라지면 진행 중인 프로필 이미지 요청을 취소한다", () => {
    AxiosApi.profileImageByUserIdGet.mockImplementation(
      () => new Promise(() => {}),
    );

    const { unmount } = render(
      <ProfileAvatar userId={12} hasImage />,
    );

    expect(AxiosApi.profileImageByUserIdGet).toHaveBeenCalledTimes(1);
    const [, signal] = AxiosApi.profileImageByUserIdGet.mock.calls[0];
    expect(signal).toBeInstanceOf(AbortSignal);
    expect(signal.aborted).toBe(false);

    unmount();

    expect(signal.aborted).toBe(true);
  });
});
