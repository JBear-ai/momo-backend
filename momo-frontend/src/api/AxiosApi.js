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
  // 7. 영화 포스터 조회 (단건)
  // =========================================================

  moviePoster: async (movieCode, signal) => {
    return await axios.get(
      `${HM_DOMAIN}/api/movies/${encodeURIComponent(movieCode)}/poster`,
      { signal },
    );
  },

  // =========================================================
  // 8. 영화 포스터 상태 일괄 조회
  // =========================================================

  moviePosterStatuses: async (movieCodes, signal) => {
    return await axios.post(
      `${HM_DOMAIN}/api/movies/posters/status`,
      movieCodes,
      { signal },
    );
  },
};

export default AxiosApi;
