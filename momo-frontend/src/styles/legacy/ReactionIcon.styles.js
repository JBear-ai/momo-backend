import styled from "styled-components";

const ButtonWrapper = styled.button`
  background: none;
  border: none;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  transition: transform 0.1s ease;

  &:hover {
    transform: scale(1.05);
  }

  img {
    width: 20px;
    height: 20px;
    object-fit: contain;
  }

  span {
    font-size: 14px;
    color: #666666;
    font-weight: 500;
  }
`;

export { ButtonWrapper };
