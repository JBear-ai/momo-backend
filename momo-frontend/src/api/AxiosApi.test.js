jest.mock("axios", () => {
  const mockedAxios = jest.fn();
  mockedAxios.post = jest.fn();
  mockedAxios.get = jest.fn();
  mockedAxios.interceptors = {
    request: { use: jest.fn() },
    response: { use: jest.fn() },
  };
  return mockedAxios;
});

import axios from "axios";
import AxiosApi, { clearAuthSession, storeLoginTokens } from "./AxiosApi";

const requestInterceptor = axios.interceptors.request.use.mock.calls[0][0];
const responseErrorInterceptor = axios.interceptors.response.use.mock.calls[0][1];
const API_URL = "http://localhost:8111/api/private";

function unauthorizedRequest() {
  return requestInterceptor({
    url: API_URL,
    headers: {},
  });
}

function unauthorizedError(config) {
  return {
    response: { status: 401 },
    config,
  };
}

describe("AxiosApi authentication interceptors", () => {
  beforeEach(() => {
    localStorage.clear();
    axios.mockReset();
    axios.post.mockReset();
    axios.mockResolvedValue({ data: { success: true } });
  });

  test("clearAuthSession removes both tokens and broadcasts one auth event", () => {
    storeLoginTokens("access-token", "refresh-token");
    const listener = jest.fn();
    window.addEventListener("momo-auth-changed", listener);

    clearAuthSession();

    expect(localStorage.getItem("accessToken")).toBeNull();
    expect(localStorage.getItem("refreshToken")).toBeNull();
    expect(localStorage.getItem("username")).toBeNull();
    expect(localStorage.getItem("nickname")).toBeNull();
    expect(listener).toHaveBeenCalledTimes(1);

    window.removeEventListener("momo-auth-changed", listener);
  });

  test("an older refresh promise cannot clear a newer session refresh promise", async () => {
    const pending = [];
    axios.post.mockImplementation(
      () =>
        new Promise((resolve) => {
          pending.push({ resolve });
        }),
    );

    storeLoginTokens("old-access", "old-refresh");
    const firstRequest = unauthorizedRequest();
    const firstRefresh = responseErrorInterceptor(
      unauthorizedError(firstRequest),
    );

    // 로그아웃 후 다른 계정으로 로그인해 새로운 재발급을 시작한다.
    clearAuthSession();
    storeLoginTokens("new-access", "new-refresh");

    const secondRequest = unauthorizedRequest();
    const secondRefresh = responseErrorInterceptor(
      unauthorizedError(secondRequest),
    );

    // 이전 세션의 응답은 새 토큰을 저장하지 못해야 한다.
    pending[0].resolve({
      data: {
        accessToken: "stale-access",
        refreshToken: "stale-refresh",
      },
    });

    await expect(firstRefresh).rejects.toMatchObject({
      code: "MOMO_AUTH_SESSION_CHANGED",
    });

    // 이전 Promise의 finally가 새 Promise를 지우지 않았는지 확인한다.
    const thirdRequest = unauthorizedRequest();
    const thirdRefresh = responseErrorInterceptor(
      unauthorizedError(thirdRequest),
    );

    expect(axios.post).toHaveBeenCalledTimes(2);

    pending[1].resolve({
      data: {
        accessToken: "fresh-access",
        refreshToken: "fresh-refresh",
      },
    });

    await Promise.all([secondRefresh, thirdRefresh]);

    expect(localStorage.getItem("accessToken")).toBe("fresh-access");
    expect(localStorage.getItem("refreshToken")).toBe("fresh-refresh");
  });

  test("temporary refresh server errors do not erase a valid local session", async () => {
    storeLoginTokens("access-token", "refresh-token");
    axios.post.mockRejectedValue({
      response: { status: 503 },
    });

    const request = unauthorizedRequest();

    await expect(
      responseErrorInterceptor(unauthorizedError(request)),
    ).rejects.toMatchObject({
      response: { status: 503 },
    });

    expect(localStorage.getItem("accessToken")).toBe("access-token");
    expect(localStorage.getItem("refreshToken")).toBe("refresh-token");
  });
});


describe("unified movie detail API", () => {
  test("uses one resolve endpoint for both KOBIS codes and TMDb identifiers", async () => {
    axios.get.mockResolvedValue({ data: { title: "테스트 영화" } });

    await AxiosApi.movieDetailByIdentifier("1571245");
    expect(axios.get).toHaveBeenCalledWith(
      "http://localhost:8111/api/movies/resolve/1571245",
      { signal: undefined },
    );

    axios.get.mockClear();
    await AxiosApi.movieDetailByIdentifier("20259894");
    expect(axios.get).toHaveBeenCalledWith(
      "http://localhost:8111/api/movies/resolve/20259894",
      { signal: undefined },
    );
  });
});


describe("abortable read APIs", () => {
  test("passes AbortSignal to member ID availability checks", async () => {
    const controller = new AbortController();
    axios.get.mockResolvedValue({ data: false });

    await AxiosApi.memberIdExist("momo-user", controller.signal);

    expect(axios.get).toHaveBeenCalledWith(
      "http://localhost:8111/api/auth/exists/id/momo-user",
      { signal: controller.signal },
    );
  });

  test("supports aborting the current user's profile image request", async () => {
    const controller = new AbortController();
    axios.get.mockResolvedValue({ data: new Blob() });

    await AxiosApi.profileImageGet(controller.signal);

    expect(axios.get).toHaveBeenCalledWith(
      "http://localhost:8111/api/auth/me/profile-image",
      {
        withCredentials: true,
        responseType: "blob",
        signal: controller.signal,
      },
    );
  });
});
