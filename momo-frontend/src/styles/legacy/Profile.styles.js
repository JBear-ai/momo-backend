import styled from "styled-components";
import { Link } from "react-router-dom";

// 회원 탈퇴 링크의 기본 링크 장식을 제거한다.
const ProfileDeleteLink = styled(Link)`
  text-decoration: none;
`;

export { ProfileDeleteLink };
