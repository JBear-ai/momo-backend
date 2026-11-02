import { useState, useEffect } from "react";
import {
  Link,
  NavLink,
  Outlet,
  useNavigate,
  useSearchParams,
} from "react-router-dom";
import styled from "styled-components";
import textlogo from "../textlogo.png";

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

  const [keyword, setKeyword] = useState(searchParams.get("keyword") || "");

  const [nickname, setNickname] = useState(
    localStorage.getItem("nickname") || "",
  );

  // URL의 검색어가 변경되면 헤더 검색창에도 반영
  useEffect(() => {
    setKeyword(searchParams.get("keyword") || "");
  }, [searchParams]);

  // 로그인 사용자 닉네임 확인
  useEffect(() => {
    const savedNickname = localStorage.getItem("nickname") || "";
    setNickname(savedNickname);
  }, []);

  // 헤더 검색 실행
  const handleSearchSubmit = (e) => {
    e.preventDefault();

    const trimmedKeyword = keyword.trim();

    if (!trimmedKeyword) return;

    navigate(`/search?keyword=${encodeURIComponent(trimmedKeyword)}`);
  };

  // 로그아웃
  const handleLogout = () => {
    localStorage.removeItem("username");
    localStorage.removeItem("nickname");

    setNickname("");

    navigate("/login");
  };

  return (
    <Container>
      <Header>
        <Logo to="/" aria-label="MOMO 홈">
          <LogoImage src={textlogo} alt="MOMO" />
        </Logo>

        <SearchForm onSubmit={handleSearchSubmit}>
          <SearchInput
            type="text"
            placeholder="영화 제목 또는 키워드 검색"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />

          <SearchButton type="submit">검색</SearchButton>
        </SearchForm>

        <Navigation>
          {nickname ? (
            <UserArea>
              <NavLink to="/profile">{nickname}님</NavLink>

              <ActionButton onClick={handleLogout}>로그아웃</ActionButton>
            </UserArea>
          ) : (
            <UserArea>
              <NavLink to="/login">로그인</NavLink>

              <NavLink to="/signup">회원가입</NavLink>
            </UserArea>
          )}
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
              <Link to="/profile">마이 페이지</Link>
            </FooterColumn>
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
