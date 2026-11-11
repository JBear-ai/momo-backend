import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getFavoriteMovies } from "../utils/favorites";
import AxiosApi from "../api/AxiosApi";
import ProfileAvatar from "../components/ProfileAvatar";

function Profile() {
  const [userId, setUserId] = useState("");
  const [nickname, setNickname] = useState("");
  const [email, setEmail] = useState("");
  const [profileUserId, setProfileUserId] = useState(null);
  const [hasProfileImage, setHasProfileImage] = useState(false);

  // 영화 활동 개수
  const [favoriteCount, setFavoriteCount] = useState(
    () => getFavoriteMovies().length,
  );
  const [reviewCount, setReviewCount] = useState(null);
  const [likedReviewCount, setLikedReviewCount] = useState(null);

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

  // 프로필 및 영화 활동 정보 조회
  useEffect(() => {
    const controller = new AbortController();

    const loadProfile = async () => {
      try {
        const response = await AxiosApi.memberMe();

        if (controller.signal.aborted) return;

        setProfileUserId(response.data.userId || null);
        setHasProfileImage(Boolean(response.data.profileImage));
      } catch (error) {
        if (error.name !== "CanceledError") {
          console.error("프로필 정보 조회 실패:", error);
        }
      }
    };

    // 작성한 리뷰와 좋아요한 리뷰의 전체 개수 조회
    const loadReviewCounts = async () => {
      const [myReviewsResult, likedReviewsResult] = await Promise.allSettled([
        AxiosApi.myReviews(0, controller.signal),
        AxiosApi.myLikedReviews(0, controller.signal),
      ]);

      if (controller.signal.aborted) return;

      // Spring Data Page 응답의 전체 데이터 개수 추출
      const getTotalCount = (result) => {
        if (result.status !== "fulfilled") return null;

        const data = result.value.data;

        return data.page?.totalElements ?? data.totalElements ?? null;
      };

      const myCount = getTotalCount(myReviewsResult);
      const likedCount = getTotalCount(likedReviewsResult);

      if (myCount !== null) {
        setReviewCount(myCount);
      }

      if (likedCount !== null) {
        setLikedReviewCount(likedCount);
      }

      if (myReviewsResult.status === "rejected") {
        console.error("작성한 리뷰 개수 조회 실패:", myReviewsResult.reason);
      }

      if (likedReviewsResult.status === "rejected") {
        console.error(
          "좋아요한 리뷰 개수 조회 실패:",
          likedReviewsResult.reason,
        );
      }
    };

    loadProfile();
    loadReviewCounts();

    return () => {
      controller.abort();
    };
  }, []);

  return (
    <main className="profile-page">
      <div className="profile-container">
        {/* 페이지 제목 */}
        <header className="profile-heading">
          <h1>마이페이지</h1>
          <p>나의 계정 정보와 영화 활동을 확인해 보세요.</p>
        </header>

        {/* 내 프로필 */}
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
              <ProfileAvatar
                userId={profileUserId}
                hasImage={hasProfileImage}
                alt="프로필 이미지"
              />
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

          {/* 영화 활동 3개 가로 배치 */}
          <div className="profile-activity-grid">
            {/* 작성한 리뷰 */}
            <Link
              to="/profile/reviews"
              className="profile-activity-item profile-reviews-link"
            >
              <span className="profile-activity-icon">✎</span>
              <h3>작성한 리뷰</h3>
              <strong>{reviewCount === null ? "-" : reviewCount} 개</strong>
              <p>작성한 리뷰 보기 →</p>
            </Link>

            {/* 좋아요한 리뷰 */}
            <Link
              to="/profile/liked-reviews"
              className="profile-activity-item profile-liked-reviews-link"
            >
              <span className="profile-activity-icon">♥</span>
              <h3>좋아요한 리뷰</h3>
              <strong>
                {likedReviewCount === null ? "-" : likedReviewCount} 개
              </strong>
              <p>좋아요한 리뷰 보기 →</p>
            </Link>

            {/* 관심 영화 */}
            <Link
              to="/profile/favorites"
              className="profile-activity-item profile-favorites-link"
            >
              <span className="profile-activity-icon">★</span>
              <h3>관심 영화</h3>
              <strong>{favoriteCount} 편</strong>
              <p>관심 영화 보기 →</p>
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
