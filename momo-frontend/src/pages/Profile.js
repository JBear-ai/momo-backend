import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getFavoriteMovies } from "../utils/favorites";
import "../App.css";
import AxiosApi from "../api/AxiosApi";

function Profile() {
  const [userId, setUserId] = useState("");
  const [nickname, setNickname] = useState("");
  const [email, setEmail] = useState("");

  // 서버에서 불러온 프로필 이미지 표시 주소
  const [profileImageUrl, setProfileImageUrl] = useState("");

  // 관심 영화 개수 관리
  const [favoriteCount, setFavoriteCount] = useState(
    () => getFavoriteMovies().length,
  );

  // 브라우저에 저장된 회원 정보 불러오기
  useEffect(() => {
    setUserId(localStorage.getItem("username") || "");
    setNickname(localStorage.getItem("nickname") || "");
    setEmail(localStorage.getItem("email") || "");
  }, []);

  // 관심 영화 등록 및 해제 상태 감지
  useEffect(() => {
    const updateFavorites = (event) => {
      const favorites = event.detail || getFavoriteMovies();
      setFavoriteCount(favorites.length);
    };

    window.addEventListener("momo-favorites-changed", updateFavorites);

    return () => {
      window.removeEventListener("momo-favorites-changed", updateFavorites);
    };
  }, []);

  // 서버에 저장된 프로필 이미지를 불러옵니다.
  useEffect(() => {
    let imageUrl = "";

    const loadProfileImage = async () => {
      try {
        const userResponse = await AxiosApi.memberMe();

        // 저장된 프로필 이미지가 있을 때 실제 이미지 데이터를 조회합니다.
        if (userResponse.data.profileImage) {
          const imageResponse = await AxiosApi.profileImageGet();

          imageUrl = URL.createObjectURL(imageResponse.data);
          setProfileImageUrl(imageUrl);
        }
      } catch (error) {
        console.error("프로필 이미지 조회 실패:", error);
      }
    };

    loadProfileImage();

    // 화면을 벗어날 때 생성한 임시 이미지 주소를 정리합니다.
    return () => {
      if (imageUrl) {
        URL.revokeObjectURL(imageUrl);
      }
    };
  }, []);

  return (
    <main className="profile-page">
      <div className="profile-container">
        {/* 마이페이지 제목 */}
        <header className="profile-heading">
          <h1>마이페이지</h1>
          <p>나의 계정 정보와 영화 활동을 확인해 보세요.</p>
        </header>

        {/* 회원 프로필 */}
        <section className="profile-card">
          <div className="profile-card-heading">
            <div>
              <h2>내 프로필</h2>
              <p>나의 회원 정보를 확인할 수 있어요.</p>
            </div>

            <div className="profile-card-actions">
              <Link to="/profile/edit" className="profile-button">
                프로필 수정
              </Link>
            </div>
          </div>

          <div className="profile-user">
            <div className="profile-avatar">
              {profileImageUrl ? (
                <img src={profileImageUrl} alt="프로필" />
              ) : (
                (nickname || userId || "M").charAt(0).toUpperCase()
              )}
            </div>

            <div className="profile-user-info">
              <h3>{nickname || userId || "로그인 사용자"}</h3>

              <p>
                <span>아이디</span>
                {userId || "정보 없음"}
              </p>

              <p>
                <span>닉네임</span>
                {nickname || "정보 없음"}
              </p>

              <p>
                <span>이메일</span>
                {email || "등록된 이메일 정보 없음"}
              </p>
            </div>
          </div>
        </section>

        {/* 나의 영화 활동 */}
        <section className="profile-card">
          <div className="profile-card-heading">
            <div>
              <h2>나의 영화 활동</h2>
              <p>내가 남긴 영화 기록을 모아볼 수 있어요.</p>
            </div>
          </div>

          <div className="profile-activity">
            {/* 작성한 리뷰 */}
            <article className="profile-activity-item">
              <span className="profile-activity-icon">✎</span>

              <div>
                <h3>작성한 리뷰</h3>
                <p>내가 작성한 영화 리뷰를 확인할 수 있어요.</p>
              </div>

              <span className="profile-coming">준비 중</span>
            </article>

            {/* 좋아요한 리뷰 */}
            <article className="profile-activity-item">
              <span className="profile-activity-icon">♥</span>

              <div>
                <h3>좋아요한 리뷰</h3>
                <p>마음에 들었던 리뷰를 모아볼 수 있어요.</p>
              </div>

              <span className="profile-coming">준비 중</span>
            </article>

            {/* 관심 영화 */}
            <Link
              to="/profile/favorites"
              className="profile-activity-item profile-favorites-link"
            >
              <span className="profile-activity-icon">★</span>

              <div>
                <h3>관심 영화</h3>
                <p>관심 있는 영화를 관리할 수 있어요.</p>
              </div>

              <span className="profile-favorites-count">
                {favoriteCount}개 보기 →
              </span>
            </Link>
          </div>
        </section>

        {/* 회원 탈퇴 */}
        <section className="profile-card profile-delete-section">
          <div>
            <h2>회원 탈퇴</h2>
            <p>회원 탈퇴를 진행하면 계정 이용에 제한이 생길 수 있습니다.</p>
          </div>

          <Link
            to="/profile/delete"
            className="profile-button profile-delete-button"
            style={{ textDecoration: "none" }}
          >
            회원 탈퇴
          </Link>
        </section>
      </div>
    </main>
  );
}

export default Profile;
