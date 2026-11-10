import React, { useState, useEffect } from "react";
import {
  Link,
  NavLink,
  Outlet,
  useNavigate,
  useSearchParams,
  useLocation,
} from "react-router-dom";
import styled from "styled-components";
import textlogo from "../textlogo.png";
import AxiosApi, { clearAuthSession } from "../api/AxiosApi";

const Container = styled.div`
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f2eb;
  color: #1c1a17;
  letter-spacing: -0.01em;
`;

const Header = styled.header`
  position: sticky;
  top: 0;
  z-index: 100;
  min-height: 76px;
  padding: 0 8%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  background-color: #ffffff;
  border-bottom: 1px solid #e2dbd0;
  box-sizing: border-box;
  box-shadow: 0 4px 20px -4px rgba(45, 38, 26, 0.03);
`;

const Logo = styled(Link)`
  display: flex;
  align-items: center;
  flex-shrink: 0;
`;

const LogoImage = styled.img`
  display: block;
  width: auto;
  height: 38px;
  object-fit: contain;
`;

const SearchForm = styled.form`
  display: flex;
  align-items: center;
  background-color: #fffaec;
  border: 1px solid #efe6d0;
  border-radius: 20px;
  padding: 6px 16px;
  width: 340px;
  max-width: 100%;
  box-sizing: border-box;
  transition: all 0.2s ease;

  &:focus-within {
    border-color: #ffd43b;
    box-shadow: 0 0 0 3px rgba(255, 212, 59, 0.2);
  }
`;

const SearchInput = styled.input`
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 14px;
  color: #1c1a17;

  &::placeholder {
    color: #948b7c;
  }
`;

const SearchButton = styled.button`
  background: none;
  border: none;
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  color: #594400;
  padding: 2px 4px;
  margin-left: 6px;

  &:hover {
    color: #1c1a17;
  }
`;

const Navigation = styled.nav`
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 24px;

  a {
    color: #787062;
    text-decoration: none;
    font-size: 14px;
    font-weight: 600;
    transition: color 0.15s ease;

    &:hover {
      color: #1c1a17;
    }
  }

  a.active {
    color: #1c1a17;
    font-weight: 800;
  }
`;

const UserArea = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
  white-space: nowrap;
  font-size: 14px;
`;

const ActionButton = styled.button`
  padding: 8px 16px;
  border: 1px solid #e2dbd0;
  border-radius: 8px;
  background-color: #ffffff;
  color: #1c1a17;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.02);
  transition: all 0.2s ease;

  &:hover {
    background-color: #fffaec;
    border-color: #ffd43b;
    transform: translateY(-1px);
  }
`;

const Main = styled.main`
  flex: 1;
  padding: 40px 8%;
  width: 100%;
  box-sizing: border-box;
`;

/* ========================================
   푸터 영역
======================================== */

const Footer = styled.footer`
  padding: 44px 8%;
  background-color: #efe8da;
  color: #595042;
  border-top: 1px solid #e2dbd0;
  box-sizing: border-box;
  width: 100%;
`;

const FooterContainer = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 40px;
  width: 100%;
  max-width: 1320px;
  margin: 0 auto 28px;
  padding-bottom: 28px;
  border-bottom: 1px solid #e2dbd0;
  box-sizing: border-box;

  @media (max-width: 768px) {
    flex-direction: column;
    align-items: flex-start;
    gap: 24px;
  }
`;

const FooterLeft = styled.div`
  display: flex;
  align-items: center;
  gap: 28px;

  @media (max-width: 600px) {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
`;

const FooterLogo = styled.img`
  display: block;
  width: auto;
  height: 28px;
  object-fit: contain;
  flex-shrink: 0;
`;

const FooterDesc = styled.p`
  margin: 0;
  color: #787062;
  font-size: 13px;
  line-height: 1.5;
  max-width: 380px;
  word-break: keep-all;
  border-left: 1px solid #e2dbd0;
  padding-left: 20px;

  @media (max-width: 600px) {
    border-left: none;
    padding-left: 0;
  }
`;

const FooterRight = styled.div`
  display: flex;
  gap: 48px;

  @media (max-width: 480px) {
    flex-direction: column;
    gap: 20px;
  }
`;

const FooterColumn = styled.div`
  display: flex;
  flex-direction: column;
  gap: 10px;

  h4 {
    margin: 0 0 4px;
    font-size: 13px;
    font-weight: 800;
    color: #1c1a17;
    letter-spacing: 0.05em;
    text-transform: uppercase;
  }

  span,
  a {
    color: #787062;
    font-size: 13px;
    text-decoration: none;
    transition: color 0.15s ease;

    &:hover {
      color: #1c1a17;
    }
  }
`;

const FooterBottom = styled.div`
  width: 100%;
  max-width: 1320px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #948b7c;
  box-sizing: border-box;

  @media (max-width: 600px) {
    flex-direction: column;
    gap: 8px;
    text-align: center;
  }
`;

function Layout() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  // 현재 페이지 경로를 확인
  // 페이지가 이동하면 로그인 상태를 다시 확인하기 위해 사용
  const location = useLocation();

  const [keyword, setKeyword] = useState(searchParams.get("keyword") || "");

  // 서버 확인 전에는 로그인 상태를 확정하지 않음
  const [nickname, setNickname] = useState("");

  // 서버 세션 확인이 끝났는지 구분
  const [authLoading, setAuthLoading] = useState(true);

  // URL의 검색어가 변경되면 헤더 검색창에도 반영
  useEffect(() => {
    setKeyword(searchParams.get("keyword") || "");
  }, [searchParams]);

  // 서버 세션을 확인하여 로그인 상태를 갱신
  useEffect(() => {
    let active = true;

    const checkLogin = async () => {
      const accessToken = localStorage.getItem("accessToken");

      if (!accessToken) {
        if (active) {
          setNickname("");
          setAuthLoading(false);
        }
        return;
      }

      try {
        // 현재 서버에 로그인된 사용자 정보 요청
        const response = await AxiosApi.memberMe();

        if (!active) return;

        // 세션이 유효하면 서버에서 받은 닉네임 표시
        setNickname(response.data.nickname || "회원");
      } catch (error) {
        if (!active) return;

        // 인증 실패(401)인 경우 로그인 상태 초기화
        if (error.response?.status === 401) {
          setNickname("");
          localStorage.removeItem("username");
          localStorage.removeItem("nickname");
        } else {
          // 서버 연결 문제는 인증 실패와 구분
          console.error("로그인 상태 확인 실패:", error);
          setNickname("");
        }
      } finally {
        if (active) {
          setAuthLoading(false);
        }
      }
    };

    // 사이트 최초 접속 및 페이지 이동 시 로그인 상태 확인
    checkLogin();

    // Login.js에서 로그인 성공 이벤트가 발생하면 다시 확인
    window.addEventListener("momo-auth-changed", checkLogin);

    return () => {
      active = false;
      window.removeEventListener("momo-auth-changed", checkLogin);
    };
    // 페이지 경로가 변경되면 서버 세션을 다시 확인
  }, [location.pathname]);

  // 헤더 검색 실행
  const handleSearchSubmit = (e) => {
    e.preventDefault();

    const trimmedKeyword = keyword.trim();

    if (!trimmedKeyword) return;

    navigate(`/search?keyword=${encodeURIComponent(trimmedKeyword)}`);
  };

  // 로그아웃 시 서버 세션도 함께 종료
  const handleLogout = async () => {
    try {
      // Spring Boot에 로그아웃 요청
      await AxiosApi.memberLogout();

      // 로그아웃 성공 후 브라우저 사용자 정보 삭제
      clearAuthSession();
      window.dispatchEvent(new Event("momo-auth-changed"));

      // 화면을 로그아웃 상태로 변경
      setNickname("");

      // 로그아웃 성공 후 메인 홈페이지로 이동
      navigate("/", { replace: true });
    } catch (error) {
      console.error("로그아웃 실패:", error);
      alert("로그아웃에 실패했습니다. 다시 시도해 주세요.");
    }
  };

  return (
    React.createElement(
  Container,
  null,
  React.createElement(
      Header,
      null,
      React.createElement(
            Logo,
            {"to":"/","aria-label":"MOMO 홈"},
            React.createElement(
                    LogoImage,
                    {"src":textlogo,"alt":"MOMO"}
                  )
          ),
      React.createElement(
            SearchForm,
            {"onSubmit":handleSearchSubmit},
            React.createElement(
                    SearchInput,
                    {"type":"text","placeholder":"영화 제목 또는 키워드 검색","value":keyword,"onChange":(e) => setKeyword(e.target.value)}
                  ),
            React.createElement(
                    SearchButton,
                    {"type":"submit"},
                    "검색"
                  )
          ),
      React.createElement(
            Navigation,
            null,
            (authLoading ? null : nickname ? (
                        React.createElement(
                    UserArea,
                    null,
                    React.createElement(
                              NavLink,
                              {"to":"/"},
                              "홈"
                            ),
                    React.createElement(
                              NavLink,
                              {"to":"/reviews"},
                              "전체 리뷰"
                            ),
                    React.createElement(
                              NavLink,
                              {"to":"/profile"},
                              "마이페이지"
                            ),
                    React.createElement(
                              "p",
                              null,
                              React.createElement(
                                          "strong",
                                          null,
                                          (nickname),
                                          "님"
                                        )
                            ),
                    React.createElement(
                              ActionButton,
                              {"onClick":handleLogout},
                              "로그아웃"
                            )
                  )
                      ) : (
                        React.createElement(
                    UserArea,
                    null,
                    React.createElement(
                              NavLink,
                              {"to":"/login"},
                              "로그인"
                            ),
                    React.createElement(
                              NavLink,
                              {"to":"/signup"},
                              "회원가입"
                            )
                  )
                      ))
          )
    ),
  React.createElement(
      Main,
      null,
      React.createElement(
            Outlet,
            null
          )
    ),
  React.createElement(
      Footer,
      null,
      React.createElement(
            FooterContainer,
            null,
            React.createElement(
                    FooterLeft,
                    null,
                    React.createElement(
                              FooterLogo,
                              {"src":textlogo,"alt":"MOMO"}
                            ),
                    React.createElement(
                              FooterDesc,
                              null,
                              "영화를 메모하다, 메모지처럼 가볍게 남기는 나만의 영화 기록 공간 MOVIEMEMO."
                            )
                  ),
            React.createElement(
                    FooterRight,
                    null,
                    React.createElement(
                              FooterColumn,
                              null,
                              React.createElement(
                                          "h4",
                                          null,
                                          "Service"
                                        ),
                              React.createElement(
                                          Link,
                                          {"to":"/"},
                                          "홈 오버뷰"
                                        ),
                              React.createElement(
                                          Link,
                                          {"to":"/reviews"},
                                          "전체 리뷰"
                                        ),
                              React.createElement(
                                          Link,
                                          {"to":"/profile"},
                                          "마이 페이지"
                                        )
                            )
                  )
          ),
      React.createElement(
            FooterBottom,
            null,
            React.createElement(
                    "p",
                    null,
                    "© 2026 MOMO. All rights reserved."
                  ),
            React.createElement(
                    "p",
                    null,
                    "Designed for Movie Lovers with Warm Analog 감성"
                  )
          )
    )
)
  );
}

export default Layout;
