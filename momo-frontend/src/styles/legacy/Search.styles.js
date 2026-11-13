import styled from "styled-components";

const SearchPage = styled.div`
  width: 100%;
  max-width: 1320px;
  margin: 0 auto;
  padding-bottom: 60px;
  color: #1c1a17;
`;

const SearchHeading = styled.div`
  margin-bottom: 32px;

  h1 {
    margin: 0 0 10px;
    font-size: 30px;
    font-weight: 800;
    letter-spacing: -0.04em;
  }

  p {
    margin: 0;
    color: #787062;
    font-size: 14px;
  }
`;

const SearchInput = styled.input`
  width: 100%;
  padding: 17px 20px;
  box-sizing: border-box;
  border: 1px solid #e2dbd0;
  border-radius: 12px;
  background: #ffffff;
  color: #1c1a17;
  font-size: 15px;
  outline: none;

  &:focus {
    border-color: #ffd43b;
    box-shadow: 0 0 0 3px rgba(255, 212, 59, 0.18);
  }

  &::placeholder {
    color: #948b7c;
  }
`;

const MovieGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 28px 18px;
  margin-top: 36px;

  @media (max-width: 1000px) {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  @media (max-width: 700px) {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 20px 12px;
  }

  @media (max-width: 430px) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
`;

const MovieCard = styled.div`
  min-width: 0;
  padding: 0;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;

  animation: appear 0.3s ease both;

  @keyframes appear {
    from {
      opacity: 0;
      transform: translateY(8px);
    }

    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  &:hover img {
    transform: scale(1.04);
  }

  &:focus-visible {
    outline: 2px solid #a37e1c;
    outline-offset: 4px;
  }
`;

const Poster = styled.div`
  position: relative;
  width: 100%;
  aspect-ratio: 2 / 2.85;
  overflow: hidden;
  border-radius: 10px;
  border: 1px solid #e2dbd0;
  background: #efece4;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
    transition: transform 0.35s ease;
  }
`;

const MovieInfo = styled.div`
  padding-top: 12px;

  h3 {
    margin: 0;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    color: #1c1a17;
    font-size: 15px;
    font-weight: 750;
  }

  p {
    margin: 6px 0 0;
    color: #787062;
    font-size: 12px;
  }
`;

const SearchMessage = styled.div`
  padding: 70px 0;
  text-align: center;
  color: #787062;
  font-size: 14px;
`;

const LoadingMessage = styled.div`
  padding: 35px 0;
  text-align: center;
  color: #787062;
  font-size: 13px;
`;

const RetryButton = styled.button`
  margin-top: 12px;
  padding: 9px 18px;
  border: 1px solid #e2dbd0;
  border-radius: 8px;
  background: #ffffff;
  color: #1c1a17;
  cursor: pointer;

  &:hover {
    background: #fff5d6;
    border-color: #ffd43b;
  }
`;

const ScrollSentinel = styled.div`
  width: 100%;
  height: 1px;
`;

export { SearchPage };
export { SearchHeading };
export { SearchInput };
export { MovieGrid };
export { MovieCard };
export { Poster };
export { MovieInfo };
export { SearchMessage };
export { LoadingMessage };
export { RetryButton };
export { ScrollSentinel };
