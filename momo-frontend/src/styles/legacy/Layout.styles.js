import styled from "styled-components";
import { Link } from "react-router-dom";

const Container = styled.div`
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f2eb;
  color: #1c1a17;
  letter-spacing: -0.01em;
`;

// ========================================
// 헤더
// ========================================

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

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
`;

const Main = styled.main`
  flex: 1;
  padding: 40px 8%;
  width: 100%;
  box-sizing: border-box;
`;

// ========================================
// 푸터
// ========================================

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

export { Container };
export { Header };
export { Logo };
export { LogoImage };
export { Navigation };
export { UserArea };
export { ActionButton };
export { Main };
export { Footer };
export { FooterContainer };
export { FooterLeft };
export { FooterLogo };
export { FooterDesc };
export { FooterRight };
export { FooterColumn };
export { FooterBottom };
