import axios from "axios";

const HM_DOMAIN = "http://localhost:8111";

const AxiosApi = {
  // 로그인 아이디 중복 확인
  memberIdExist: async (loginId) => {
    return await axios.get(
      `${HM_DOMAIN}/api/auth/exists/id/${encodeURIComponent(loginId)}`,
    );
  },

  // 회원가입
  memberSignup: async (loginId, password, nickname, email) => {
    return await axios.post(`${HM_DOMAIN}/api/auth/signup`, {
      loginId,
      password,
      nickname,
      email,
    });
  },

  // 로그인 (기존꺼)
  // memberLogin: async (loginId, password) => {
  //   return await axios.post(`${HM_DOMAIN}/api/auth/login`, {
  //     loginId,
  //     password,
  //   });
  // },

  // 로그인 (수정 사항 / 11.02)
  memberLogin: async (loginId, password) => {
    return await axios.post(
      `${HM_DOMAIN}/api/auth/login`,
      { loginId, password },
      { withCredentials: true },
    );
  },

  // 회원정보 수정
  memberUpdate: async (data) => {
    return await axios.put(`${HM_DOMAIN}/api/auth/me`, data, {
      withCredentials: true,
    });
  },

  // 회원 탈퇴 (추가 / 11.02)
  memberDelete: async (password) => {
    return await axios.delete(`${HM_DOMAIN}/api/auth/me`, {
      data: { password },
      withCredentials: true,
    });
  },
};

export default AxiosApi;
