import React, { useState, useEffect } from "react";
import {
  Link,
  NavLink,
  Outlet,
  useNavigate,
  useLocation,
} from "react-router-dom";
import {
  Container,
  Header,
  Logo,
  LogoImage,
  Navigation,
  UserArea,
  ActionButton,
  Main,
  Footer,
  FooterContainer,
  FooterLeft,
  FooterLogo,
  FooterDesc,
  FooterRight,
  FooterColumn,
  FooterBottom,
} from "../styles/legacy/Layout.styles";

import textlogo from "../textlogo.png";
import AxiosApi, { clearAuthSession } from "../api/AxiosApi";
import { getCurrentFavoriteUserId, syncFavoriteMoviesFromServer } from "../utils/favorites";
import MovieSearchAutocomplete from "../components/common/MovieSearchAutocomplete";

// ========================================
// Layout 컴포넌트
// ========================================

function Layout() {
  const navigate = useNavigate();
  const location = useLocation();

  const [nickname, setNickname] = useState("");
  const [authLoading, setAuthLoading] = useState(true);
  const [logoutLoading, setLogoutLoading] = useState(false);

  // ========================================
  // 로그인한 회원의 관심 영화는 서버를 기준으로 동기화한다.
  useEffect(() => {
    const syncFavorites = () => {
      const currentUserId = getCurrentFavoriteUserId();
      if (!currentUserId) return;

      syncFavoriteMoviesFromServer(currentUserId).catch((error) => {
        console.error("관심 영화 동기화 실패:", error);
      });
    };

    syncFavorites();
    window.addEventListener("momo-auth-changed", syncFavorites);

    return () => {
      window.removeEventListener("momo-auth-changed", syncFavorites);
    };
  }, []);

  // 서버 로그인 상태 확인
  // ========================================

  useEffect(() => {
    let active = true;
    let activeController = null;

    const checkLogin = async () => {
      if (activeController) activeController.abort();
      const controller = new AbortController();
      activeController = controller;
      const accessToken = localStorage.getItem("accessToken");

      if (!accessToken) {
        if (active) {
          setNickname("");
          setAuthLoading(false);
        }
        return;
      }

      try {
        const response = await AxiosApi.memberMe(controller.signal);

        if (!active || controller.signal.aborted) return;

        setNickname(response.data.nickname || "회원");
      } catch (error) {
        if (!active || controller.signal.aborted || error.code === "ERR_CANCELED" || error.name === "CanceledError") return;

        if (error.response?.status === 401) {
          // 인증 만료 처리는 AxiosApi 인터셉터가 담당
          setNickname("");
        } else {
          // 서버 연결 오류와 인증 실패를 구분
          console.error("로그인 상태 확인 실패:", error);
          setNickname("");
        }
      } finally {
        if (active && !controller.signal.aborted) {
          setAuthLoading(false);
        }
      }
    };

    checkLogin();

    // 인증 상태가 바뀌면 관심 영화 화면도 현재 회원 기준으로 다시 읽는다.
    const handleAuthChanged = () => {
      window.dispatchEvent(new Event("momo-favorites-changed"));
      checkLogin();
    };

    window.addEventListener("momo-auth-changed", handleAuthChanged);

    return () => {
      active = false;
      if (activeController) activeController.abort();
      window.removeEventListener("momo-auth-changed", handleAuthChanged);
    };
  }, [location.pathname]);

  // ========================================
  // 로그아웃
  // ========================================

  const handleLogout = async () => {
    if (logoutLoading) return;

    setLogoutLoading(true);

    try {
      // Spring Boot 서버 로그아웃 요청
      await AxiosApi.memberLogout();

      // Access Token과 Refresh Token을 포함한 인증 정보를 한 번에 정리한다.
      clearAuthSession();

      // 메인 화면 이동
      navigate("/", { replace: true });
    } catch (error) {
      console.error("로그아웃 실패:", error);

      // 401은 AxiosApi 인증 인터셉터가 처리하므로
      // 중복 알림을 표시하지 않음
      if (error.response?.status !== 401) {
        alert("로그아웃에 실패했습니다. 다시 시도해 주세요.");
      }
    } finally {
      setLogoutLoading(false);
    }
  };

  // ========================================
  // 화면
  // ========================================

  return (
    <Container>
      <Header>
        <Logo to="/" aria-label="MOMO 홈">
          <LogoImage src={textlogo} alt="MOMO" />
        </Logo>

        {/* 영화 검색 자동완성 공통 컴포넌트 */}
        <MovieSearchAutocomplete />

        <Navigation>
          {!authLoading &&
            (nickname ? (
              <UserArea>
                <NavLink to="/">홈</NavLink>

                <NavLink to="/reviews">전체 리뷰</NavLink>

                <NavLink to="/profile">마이페이지</NavLink>

                <p>
                  <strong>{nickname}</strong>님
                </p>

                <ActionButton onClick={handleLogout} disabled={logoutLoading}>
                  {logoutLoading ? "로그아웃 중..." : "로그아웃"}
                </ActionButton>
              </UserArea>
            ) : (
              <UserArea>
                <NavLink to="/login">로그인</NavLink>

                <NavLink to="/signup">회원가입</NavLink>
              </UserArea>
            ))}
        </Navigation>
      </Header>

      <Main>
        <Outlet />
      </Main>

      <Footer>
        <FooterContainer>
          <FooterLeft>
            <FooterLogo src={textlogo} alt="MOMO" />

            <FooterDesc>
              영화를 메모하다, 메모지처럼 가볍게 남기는 나만의 영화 기록 공간
              MOVIEMEMO.
            </FooterDesc>
          </FooterLeft>

          <FooterRight>
            <FooterColumn>
              <h4>Service</h4>

              <Link to="/">홈 오버뷰</Link>

              <Link to="/reviews">전체 리뷰</Link>

              <Link to="/profile">마이 페이지</Link>
            </FooterColumn>

            {nickname && (
              <FooterColumn>
                <h4>My Activity</h4>

                <Link to="/profile/reviews">작성한 리뷰</Link>

                <Link to="/profile/liked-reviews">좋아요한 리뷰</Link>
              </FooterColumn>
            )}
          </FooterRight>
        </FooterContainer>

        <FooterBottom>
          <p>© 2026 MOMO. All rights reserved.</p>

          <p>Designed for Movie Lovers with Warm Analog 감성</p>
        </FooterBottom>
      </Footer>
    </Container>
  );
}

export default Layout;
