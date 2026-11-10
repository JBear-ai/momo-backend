import axios from "axios";

// 환경변수가 있으면 팀 공용 백엔드 주소를 사용하고,
// 없으면 기존 로컬 개발 주소를 그대로 사용한다.
const HM_DOMAIN =
  process.env.REACT_APP_API_BASE_URL || "http://localhost:8111";

// 재발급 요청은 일반 axios 인터셉터에 다시 걸리지 않도록 별도 인스턴스를 사용한다.
const reissueClient = axios.create();

let refreshPromise = null;
let refreshPromiseSessionVersion = null;
let authSessionVersion = 0;

// 로그인 성공 시 Access Token과 Refresh Token을 한 세션으로 저장한다.
export const storeLoginTokens = (accessToken, refreshToken) => {
  authSessionVersion += 1;
  localStorage.setItem("accessToken", accessToken);
  localStorage.setItem("refreshToken", refreshToken);
  window.__momoAuthExpiredHandled = false;
};

// 로그아웃·회원 탈퇴·인증 만료 시 토큰과 사용자 정보를 함께 제거한다.
export const clearAuthSession = () => {
  authSessionVersion += 1;
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("username");
  localStorage.removeItem("nickname");
};

const setAuthorization = (config, accessToken) => {
  config.headers = config.headers || {};

  if (typeof config.headers.set === "function") {
    config.headers.set("Authorization", `Bearer ${accessToken}`);
  } else {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
};

const isExcludedAuthRequest = (url = "") =>
  url.endsWith("/api/auth/login") ||
  url.endsWith("/api/auth/signup") ||
  url.endsWith("/api/auth/reissue");

// 인증 만료 처리는 한 번만 수행한다.
const handleAuthExpired = (error) => {
  if (!window.__momoAuthExpiredHandled) {
    window.__momoAuthExpiredHandled = true;
    clearAuthSession();
    window.dispatchEvent(new Event("momo-auth-changed"));
    alert(
      "로그인 인증이 만료되었거나 유효하지 않습니다. 다시 로그인해 주세요.",
    );

    if (window.location.pathname !== "/login") {
      window.location.replace("/login");
    }
  }

  if (error && typeof error === "object") {
    error.momoAuthExpiredHandled = true;
  }
};

// 같은 로그인 세션에서 발생한 동시 401은 하나의 Rotation 결과를 공유한다.
const reissueAccessToken = () => {
  const refreshToken = localStorage.getItem("refreshToken");
  const requestedSessionVersion = authSessionVersion;

  if (
    refreshPromise &&
    refreshPromiseSessionVersion === requestedSessionVersion
  ) {
    return refreshPromise;
  }

  if (!refreshToken) {
    return Promise.reject(new Error("Refresh Token이 없습니다."));
  }

  let currentPromise;
  currentPromise = reissueClient
    .post(`${HM_DOMAIN}/api/auth/reissue`, { refreshToken })
    .then((response) => {
      const newAccessToken = response.data?.accessToken;
      const newRefreshToken = response.data?.refreshToken;

      if (!newAccessToken || !newRefreshToken) {
        throw new Error("재발급 응답에 토큰이 없습니다.");
      }

      // 요청 도중 로그인·로그아웃·탈퇴가 발생했다면 이전 토큰을 저장하지 않는다.
      if (
        authSessionVersion !== requestedSessionVersion ||
        localStorage.getItem("refreshToken") !== refreshToken
      ) {
        const sessionChangedError = new Error(
          "인증 상태가 이미 변경되었습니다.",
        );
        sessionChangedError.code = "MOMO_AUTH_SESSION_CHANGED";
        throw sessionChangedError;
      }

      localStorage.setItem("accessToken", newAccessToken);
      localStorage.setItem("refreshToken", newRefreshToken);

      return newAccessToken;
    })
    .finally(() => {
      if (refreshPromise === currentPromise) {
        refreshPromise = null;
        refreshPromiseSessionVersion = null;
      }
    });

  refreshPromise = currentPromise;
  refreshPromiseSessionVersion = requestedSessionVersion;
  return currentPromise;
};

// MOMO API에만 JWT를 전달한다. 외부 API 통신에는 Authorization을 강제로 붙이지 않는다.
axios.interceptors.request.use(
  (config) => {
    const accessToken = localStorage.getItem("accessToken");
    const isMomoApi = config.url?.startsWith(`${HM_DOMAIN}/api/`);
    const isPublicAuthRequest = isExcludedAuthRequest(config.url);

    if (isMomoApi && accessToken && !isPublicAuthRequest) {
      setAuthorization(config, accessToken);
      config._momoAuthSessionVersion = authSessionVersion;
    }

    return config;
  },
  (error) => Promise.reject(error),
);

// Access Token 만료 시 Refresh Token Rotation 후 원래 요청을 한 번만 재시도한다.
axios.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error.response?.status;
    const originalRequest = error.config;
    const requestUrl = originalRequest?.url || "";
    const sentHeaders = originalRequest?.headers;
    const sentAuthorization =
      sentHeaders?.Authorization || sentHeaders?.get?.("Authorization");

    if (
      status !== 401 ||
      !originalRequest ||
      !requestUrl.startsWith(`${HM_DOMAIN}/api/`) ||
      isExcludedAuthRequest(requestUrl) ||
      !sentAuthorization
    ) {
      return Promise.reject(error);
    }

    // 이전 로그인 세션의 응답이 새 로그인 세션에 영향을 주지 않도록 차단한다.
    if (
      originalRequest._momoAuthSessionVersion !== undefined &&
      originalRequest._momoAuthSessionVersion !== authSessionVersion
    ) {
      const sessionChangedError = new Error("인증 상태가 이미 변경되었습니다.");
      sessionChangedError.code = "MOMO_AUTH_SESSION_CHANGED";
      return Promise.reject(sessionChangedError);
    }

    if (originalRequest._retry) {
      handleAuthExpired(error);
      return Promise.reject(error);
    }

    originalRequest._retry = true;

    const currentAccessToken = localStorage.getItem("accessToken");

    // 다른 요청이 먼저 재발급을 마쳤다면 최신 Access Token으로 재시도한다.
    if (
      currentAccessToken &&
      sentAuthorization !== `Bearer ${currentAccessToken}`
    ) {
      setAuthorization(originalRequest, currentAccessToken);
      return axios(originalRequest);
    }

    const requestedSessionVersion = authSessionVersion;

    try {
      const newAccessToken = await reissueAccessToken();

      if (authSessionVersion !== requestedSessionVersion) {
        const sessionChangedError = new Error(
          "인증 상태가 이미 변경되었습니다.",
        );
        sessionChangedError.code = "MOMO_AUTH_SESSION_CHANGED";
        throw sessionChangedError;
      }

      setAuthorization(originalRequest, newAccessToken);
      return axios(originalRequest);
    } catch (reissueError) {
      if (reissueError?.code !== "MOMO_AUTH_SESSION_CHANGED") {
        handleAuthExpired(reissueError);
      }

      return Promise.reject(reissueError);
    }
  },
);

const AxiosApi = {
  // 1. 회원 아이디 중복 확인
  memberIdExist: async (loginId) => {
    return await axios.get(
      `${HM_DOMAIN}/api/auth/exists/id/${encodeURIComponent(loginId)}`,
    );
  },

  // 2. 회원가입
  memberSignup: async (loginId, password, nickname, email) => {
    return await axios.post(`${HM_DOMAIN}/api/auth/signup`, {
      loginId,
      password,
      nickname,
      email,
    });
  },

  // 3. 로그인
  memberLogin: async (loginId, password) => {
    return await axios.post(
      `${HM_DOMAIN}/api/auth/login`,
      { loginId, password },
      { withCredentials: true },
    );
  },

  // 4. 회원정보 수정
  memberUpdate: async (data) => {
    return await axios.put(`${HM_DOMAIN}/api/auth/me`, data, {
      withCredentials: true,
    });
  },

  // 5. 회원 탈퇴
  memberDelete: async (password) => {
    return await axios.delete(`${HM_DOMAIN}/api/auth/me`, {
      data: { password },
      withCredentials: true,
    });
  },

  // 6. 영화 검색
  movieSearch: async (keyword, page, size = 20, signal) => {
    return await axios.get(`${HM_DOMAIN}/api/movies/search`, {
      params: {
        keyword,
        page,
        size,
      },
      signal,
    });
  },

  // 6-1. 일일 박스오피스 조회
  dailyBoxOffice: async (targetDate, signal) => {
    return await axios.get(`${HM_DOMAIN}/api/boxoffice/daily`, {
      params: { targetDate },
      signal,
    });
  },

  // 6-2. 영화 상세정보 조회
  movieDetail: async (movieCode, signal) => {
    return await axios.get(
      `${HM_DOMAIN}/api/movies/${encodeURIComponent(movieCode)}`,
      { signal },
    );
  },

  // 7. 장르별 영화 목록 조회
  moviesByGenre: async (genre, page, signal) => {
    return await axios.get(`${HM_DOMAIN}/api/movies/genre`, {
      params: {
        genre,
        page,
      },
      signal,
    });
  },

  // 7-1. 박스오피스 진입 이력이 확인된 추천 후보 조회
  boxOfficeRecommendations: async (genre, signal) => {
    return await axios.get(`${HM_DOMAIN}/api/movies/recommendations`, {
      params: genre ? { genre } : {},
      signal,
    });
  },

  // 8. 영화 포스터 조회
  moviePoster: async (movieCode, signal) => {
    return await axios.get(
      `${HM_DOMAIN}/api/movies/${encodeURIComponent(movieCode)}/poster`,
      { signal },
    );
  },

  // 9. 영화 포스터 상태 일괄 조회
  moviePosterStatuses: async (movieCodes, signal) => {
    return await axios.post(
      `${HM_DOMAIN}/api/movies/posters/status`,
      movieCodes,
      { signal },
    );
  },

  // 10. 영화별 리뷰 조회
  movieReviews: async (movieCode, page = 0, signal) => {
    return await axios.get(
      `${HM_DOMAIN}/api/reviews/movie/${encodeURIComponent(movieCode)}`,
      {
        params: { page },
        signal,
      },
    );
  },

  // 11. 영화 평가 조회
  movieRating: async (movieCode, signal) => {
    return await axios.get(
      `${HM_DOMAIN}/api/movies/${encodeURIComponent(movieCode)}/rating`,
      { signal },
    );
  },

  // 11-1. 로그인 회원의 좋아요 영화 목록 조회
  myLikedMovies: async (signal) => {
    return await axios.get(`${HM_DOMAIN}/api/movies/my/liked`, {
      withCredentials: true,
      signal,
    });
  },

  // 12. 영화 좋아요 / 싫어요 저장 및 변경
  // true = 좋아요, false = 싫어요
  movieRatingSave: async (movieCode, liked) => {
    return await axios.put(
      `${HM_DOMAIN}/api/movies/${encodeURIComponent(movieCode)}/rating`,
      liked,
      {
        withCredentials: true,
        headers: {
          "Content-Type": "application/json",
        },
      },
    );
  },

  // 13. 영화 평가 취소
  movieRatingDelete: async (movieCode) => {
    return await axios.delete(
      `${HM_DOMAIN}/api/movies/${encodeURIComponent(movieCode)}/rating`,
      { withCredentials: true },
    );
  },

  // 14. 리뷰 작성
  reviewCreate: async (movieCode, title, content) => {
    return await axios.post(
      `${HM_DOMAIN}/api/reviews`,
      { movieCode, title, content },
      { withCredentials: true },
    );
  },

  // 15. 로그인 사용자 정보 조회
  memberMe: async () => {
    return await axios.get(`${HM_DOMAIN}/api/auth/me`, {
      withCredentials: true,
    });
  },

  // 16. 로그아웃
  memberLogout: async () => {
    return await axios.post(
      `${HM_DOMAIN}/api/auth/logout`,
      {},
      { withCredentials: true },
    );
  },

  // 17. 현재 비밀번호 확인
  memberPasswordCheck: async (currentPassword) => {
    return await axios.post(
      `${HM_DOMAIN}/api/auth/me/password-check`,
      { currentPassword },
      { withCredentials: true },
    );
  },

  // 18. 전체 리뷰 목록 조회
  allReviews: async (page = 0, signal, searchType = "ALL", keyword = "") => {
    return await axios.get(`${HM_DOMAIN}/api/reviews`, {
      params: { page, searchType, keyword },
      withCredentials: true,
      signal,
    });
  },

  // 18-1. 내가 작성한 리뷰 목록 조회
  myReviews: async (page = 0, signal) => {
    return await axios.get(`${HM_DOMAIN}/api/reviews/me`, {
      params: { page },
      withCredentials: true,
      signal,
    });
  },

  // 19. 리뷰 상세 조회
  reviewDetail: async (reviewId, signal) => {
    return await axios.get(
      `${HM_DOMAIN}/api/reviews/${encodeURIComponent(reviewId)}`,
      {
        withCredentials: true,
        signal,
      },
    );
  },

  // 19-1. 리뷰 수정
  reviewUpdate: async (reviewId, title, content) => {
    return await axios.put(
      `${HM_DOMAIN}/api/reviews/${encodeURIComponent(reviewId)}`,
      { title, content },
      { withCredentials: true },
    );
  },

  // 19-2. 리뷰 삭제
  reviewDelete: async (reviewId) => {
    return await axios.delete(
      `${HM_DOMAIN}/api/reviews/${encodeURIComponent(reviewId)}`,
      { withCredentials: true },
    );
  },

  // 20. 프로필 이미지 업로드 및 변경
  profileImageUpload: async (file) => {
    const formData = new FormData();

    // 선택한 이미지 파일을 multipart 형식으로 전달
    formData.append("file", file);

    return await axios.post(
      `${HM_DOMAIN}/api/auth/me/profile-image`,
      formData,
      {
        withCredentials: true,
      },
    );
  },

  // 21. 프로필 이미지 삭제
  profileImageDelete: async () => {
    return await axios.delete(`${HM_DOMAIN}/api/auth/me/profile-image`, {
      withCredentials: true,
    });
  },

  // 22. 프로필 이미지 조회
  profileImageGet: async () => {
    return await axios.get(`${HM_DOMAIN}/api/auth/me/profile-image`, {
      withCredentials: true,
      responseType: "blob",
    });
  },

  // 회원 ID로 다른 회원의 프로필 이미지 조회
  profileImageByUserIdGet: async (userId, signal) => {
    return await axios.get(
      `${HM_DOMAIN}/api/auth/users/${encodeURIComponent(userId)}/profile-image`,
      { responseType: "blob", signal },
    );
  },

  // 23. 인물 상세 및 필모그래피 조회
  getPersonDetail: async (type, name, signal) => {
    return await axios.get(
      HM_DOMAIN +
        "/api/persons/" +
        encodeURIComponent(type) +
        "/" +
        encodeURIComponent(name),
      { signal },
    );
  },

  // ========================================
  // MOMO 리뷰 커뮤니티 API
  // ========================================

  // 24. 리뷰 댓글 목록 조회
  reviewComments: async (reviewId, page = 0, signal) => {
    return await axios.get(
      `${HM_DOMAIN}/api/reviews/${encodeURIComponent(reviewId)}/comments`,
      {
        params: { page },
        withCredentials: true,
        signal,
      },
    );
  },

  // 25. 리뷰 댓글 작성
  commentCreate: async (reviewId, content) => {
    return await axios.post(
      `${HM_DOMAIN}/api/reviews/${encodeURIComponent(reviewId)}/comments`,
      { content },
      { withCredentials: true },
    );
  },

  // 26. 리뷰 댓글 수정
  commentUpdate: async (commentId, content) => {
    return await axios.put(
      `${HM_DOMAIN}/api/comments/${encodeURIComponent(commentId)}`,
      { content },
      { withCredentials: true },
    );
  },

  // 27. 리뷰 댓글 삭제
  commentDelete: async (commentId) => {
    return await axios.delete(
      `${HM_DOMAIN}/api/comments/${encodeURIComponent(commentId)}`,
      { withCredentials: true },
    );
  },

  // 27-1. 댓글 좋아요 / 싫어요 저장 및 변경
  commentReaction: async (commentId, liked) => {
    return await axios.put(
      `${HM_DOMAIN}/api/comments/${encodeURIComponent(commentId)}/reaction`,
      liked,
      {
        withCredentials: true,
        headers: { "Content-Type": "application/json" },
      },
    );
  },

  // 27-2. 댓글 반응 취소
  commentReactionDelete: async (commentId) => {
    return await axios.delete(
      `${HM_DOMAIN}/api/comments/${encodeURIComponent(commentId)}/reaction`,
      { withCredentials: true },
    );
  },

  // 28. 리뷰 반응 개수 조회
  reviewReactionCounts: async (reviewId, signal) => {
    return await axios.get(
      `${HM_DOMAIN}/api/reviews/${encodeURIComponent(reviewId)}/reactions`,
      {
        withCredentials: true,
        signal,
      },
    );
  },

  // 로그인 회원의 특정 리뷰 반응 조회 (true=좋아요, false=싫어요, null=반응 없음)
  reviewMyReactionGet: async (reviewId, signal) => {
    return await axios.get(
      `${HM_DOMAIN}/api/reviews/${encodeURIComponent(reviewId)}/reaction`,
      { withCredentials: true, signal },
    );
  },

  // 29. 리뷰 좋아요 / 싫어요 저장 및 변경
  // true = 좋아요, false = 싫어요
  reviewReaction: async (reviewId, liked) => {
    return await axios.put(
      `${HM_DOMAIN}/api/reviews/${encodeURIComponent(reviewId)}/reaction`,
      liked,
      {
        withCredentials: true,
        headers: {
          "Content-Type": "application/json",
        },
      },
    );
  },

  // 30. 리뷰 반응 취소
  reviewReactionDelete: async (reviewId) => {
    return await axios.delete(
      `${HM_DOMAIN}/api/reviews/${encodeURIComponent(reviewId)}/reaction`,
      { withCredentials: true },
    );
  },

  // 31.로그인 회원이 좋아요한 리뷰 목록 조회
  myLikedReviews: async (page = 0, signal) => {
    return await axios.get(`${HM_DOMAIN}/api/reviews/me/liked`, {
      params: { page },
      withCredentials: true,
      signal,
    });
  },
};

export default AxiosApi;
