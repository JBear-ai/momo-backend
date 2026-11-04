import axios from "axios";

const HM_DOMAIN = "http://localhost:8111";

const AxiosApi = {
  // =========================================================
  // 1. 회원 아이디 중복 확인
  // =========================================================

  memberIdExist: async (loginId) => {
    return await axios.get(
      `${HM_DOMAIN}/api/auth/exists/id/${encodeURIComponent(loginId)}`,
    );
  },

  // =========================================================
  // 2. 회원가입
  // =========================================================

  memberSignup: async (loginId, password, nickname, email) => {
    return await axios.post(`${HM_DOMAIN}/api/auth/signup`, {
      loginId,
      password,
      nickname,
      email,
    });
  },

  // =========================================================
  // 3. 로그인
  // =========================================================

  memberLogin: async (loginId, password) => {
    return await axios.post(
      `${HM_DOMAIN}/api/auth/login`,
      { loginId, password },
      { withCredentials: true },
    );
  },

  // =========================================================
  // 4. 회원정보 수정
  // =========================================================

  memberUpdate: async (data) => {
    return await axios.put(`${HM_DOMAIN}/api/auth/me`, data, {
      withCredentials: true,
    });
  },

  // =========================================================
  // 5. 회원 탈퇴
  // =========================================================

  memberDelete: async (password) => {
    return await axios.delete(`${HM_DOMAIN}/api/auth/me`, {
      data: { password },
      withCredentials: true,
    });
  },

  // =========================================================
  // 6. 영화 검색
  // =========================================================

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

  // =========================================================
  // 7. 장르별 영화 목록 조회
  // =========================================================

  moviesByGenre: async (genre, page, signal) => {
    return await axios.get(`${HM_DOMAIN}/api/movies/genre`, {
      params: {
        genre,
        page,
      },
      signal,
    });
  },

  // =========================================================
  // 8. 영화 포스터 조회
  // =========================================================

  moviePoster: async (movieCode, signal) => {
    return await axios.get(
      `${HM_DOMAIN}/api/movies/${encodeURIComponent(movieCode)}/poster`,
      { signal },
    );
  },

  // =========================================================
  // 9. 영화 포스터 상태 일괄 조회
  // =========================================================

  moviePosterStatuses: async (movieCodes, signal) => {
    return await axios.post(
      `${HM_DOMAIN}/api/movies/posters/status`,
      movieCodes,
      { signal },
    );
  },

  // =========================================================
  // 10. 영화별 리뷰 조회
  // =========================================================

  movieReviews: async (movieCode, page = 0, signal) => {
    return await axios.get(
      `${HM_DOMAIN}/api/reviews/movie/${encodeURIComponent(movieCode)}`,
      {
        params: { page },
        signal,
      },
    );
  },

  // =========================================================
  // 11. 영화 평가 조회
  // =========================================================

  movieRating: async (movieCode, signal) => {
    return await axios.get(
      `${HM_DOMAIN}/api/movies/${encodeURIComponent(movieCode)}/rating`,
      { signal },
    );
  },

  // =========================================================
  // 12. 영화 좋아요 / 싫어요 저장 및 변경
  // =========================================================

  movieRatingSave: async (movieCode, liked) => {
    return await axios.put(
      `${HM_DOMAIN}/api/movies/${encodeURIComponent(movieCode)}/rating`,
      liked,
      { withCredentials: true },
    );
  },

  // =========================================================
  // 13. 영화 평가 취소
  // =========================================================

  movieRatingDelete: async (movieCode) => {
    return await axios.delete(
      `${HM_DOMAIN}/api/movies/${encodeURIComponent(movieCode)}/rating`,
      { withCredentials: true },
    );
  },

  // =========================================================
  // 14. 리뷰 작성
  // =========================================================

  reviewCreate: async (movieCode, title, content) => {
    return await axios.post(
      `${HM_DOMAIN}/api/reviews`,
      { movieCode, title, content },
      { withCredentials: true },
    );
  },

  // =========================================================
  // 15. 로그인 사용자 정보 조회
  // =========================================================

  memberMe: async () => {
    return await axios.get(`${HM_DOMAIN}/api/auth/me`, {
      withCredentials: true,
    });
  },

  // =========================================================
  // 16. 로그아웃
  // =========================================================

  memberLogout: async () => {
    return await axios.post(
      `${HM_DOMAIN}/api/auth/logout`,
      {},
      { withCredentials: true },
    );
  },

  // =========================================================
  // 17. 현재 비밀번호 확인
  // =========================================================

  memberPasswordCheck: async (currentPassword) => {
    return await axios.post(
      `${HM_DOMAIN}/api/auth/me/password-check`,
      { currentPassword },
      { withCredentials: true },
    );
  },

  // =========================================================
  // 18. 전체 리뷰 목록 조회
  // =========================================================

  allReviews: async (page = 0, signal) => {
    return await axios.get(`${HM_DOMAIN}/api/reviews`, {
      params: { page },
      withCredentials: true,
      signal,
    });
  },

  // =========================================================
  // 19. 리뷰 상세 조회
  // =========================================================

  reviewDetail: async (reviewId, signal) => {
    return await axios.get(
      `${HM_DOMAIN}/api/reviews/${encodeURIComponent(reviewId)}`,
      {
        withCredentials: true,
        signal,
      },
    );
  },
};

export default AxiosApi;
