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
  // 6. 영화 검색 (무한 스크롤)
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
  // 8. 영화 포스터 조회 (단건)
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
  // 10. 영화별 리뷰 및 평점
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

  movieRating: async (movieCode, signal) => {
    return await axios.get(
      `${HM_DOMAIN}/api/movies/${encodeURIComponent(movieCode)}/rating`,
      { signal },
    );
  },

  reviewCreate: async (movieCode, title, content, liked) => {
    return await axios.post(
      `${HM_DOMAIN}/api/reviews`,
      { movieCode, title, content, liked },
      { withCredentials: true },
    );
  },

  // 현재 로그인한 사용자 정보 조회
  // 서버 세션이 유효한지 확인할 때 사용
  memberMe: async () => {
    return await axios.get(`${HM_DOMAIN}/api/auth/me`, {
      withCredentials: true, // 로그인 세션 쿠키 전달
    });
  },

  // 로그아웃 요청
  // 브라우저뿐 아니라 Spring Boot 서버의 세션도 종료
  memberLogout: async () => {
    return await axios.post(
      `${HM_DOMAIN}/api/auth/logout`,
      {},
      { withCredentials: true }, // 로그아웃할 세션 쿠키 전달
    );
  },

  // 현재 비밀번호 일치 여부 확인
  memberPasswordCheck: async (currentPassword) => {
    return await axios.post(
      `${HM_DOMAIN}/api/auth/me/password-check`,
      { currentPassword },
      { withCredentials: true },
    );
  },
};

export default AxiosApi;
