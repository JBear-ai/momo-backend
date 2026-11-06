import React, { useEffect, useState } from "react";
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
    React.createElement(
  "main",
  {"className":"profile-page"},
  React.createElement(
      "div",
      {"className":"profile-container"},
      React.createElement(
            "header",
            {"className":"profile-heading"},
            React.createElement(
                    "h1",
                    null,
                    "마이페이지"
                  ),
            React.createElement(
                    "p",
                    null,
                    "나의 계정 정보와 영화 활동을 확인해 보세요."
                  )
          ),
      React.createElement(
            "section",
            {"className":"profile-card"},
            React.createElement(
                    "div",
                    {"className":"profile-card-heading"},
                    React.createElement(
                              "div",
                              null,
                              React.createElement(
                                          "h2",
                                          null,
                                          "내 프로필"
                                        ),
                              React.createElement(
                                          "p",
                                          null,
                                          "나의 회원 정보를 확인할 수 있어요."
                                        )
                            ),
                    React.createElement(
                              "div",
                              {"className":"profile-card-actions"},
                              React.createElement(
                                          Link,
                                          {"to":"/profile/edit","className":"profile-button"},
                                          "프로필 수정"
                                        )
                            )
                  ),
            React.createElement(
                    "div",
                    {"className":"profile-user"},
                    React.createElement(
                              "div",
                              {"className":"profile-avatar"},
                              (profileImageUrl ? (
                                              React.createElement(
                                          "img",
                                          {"src":profileImageUrl,"alt":"프로필"}
                                        )
                                            ) : (
                                              (nickname || userId || "M").charAt(0).toUpperCase()
                                            ))
                            ),
                    React.createElement(
                              "div",
                              {"className":"profile-user-info"},
                              React.createElement(
                                          "h3",
                                          null,
                                          (nickname || userId || "로그인 사용자")
                                        ),
                              React.createElement(
                                          "p",
                                          null,
                                          React.createElement(
                                                        "span",
                                                        null,
                                                        "아이디"
                                                      ),
                                          (userId || "정보 없음")
                                        ),
                              React.createElement(
                                          "p",
                                          null,
                                          React.createElement(
                                                        "span",
                                                        null,
                                                        "닉네임"
                                                      ),
                                          (nickname || "정보 없음")
                                        ),
                              React.createElement(
                                          "p",
                                          null,
                                          React.createElement(
                                                        "span",
                                                        null,
                                                        "이메일"
                                                      ),
                                          (email || "등록된 이메일 정보 없음")
                                        )
                            )
                  )
          ),
      React.createElement(
            "section",
            {"className":"profile-card"},
            React.createElement(
                    "div",
                    {"className":"profile-card-heading"},
                    React.createElement(
                              "div",
                              null,
                              React.createElement(
                                          "h2",
                                          null,
                                          "나의 영화 활동"
                                        ),
                              React.createElement(
                                          "p",
                                          null,
                                          "내가 남긴 영화 기록을 모아볼 수 있어요."
                                        )
                            )
                  ),
            React.createElement(
                    "div",
                    {"className":"profile-activity"},
                    React.createElement(
                              "article",
                              {"className":"profile-activity-item"},
                              React.createElement(
                                          "span",
                                          {"className":"profile-activity-icon"},
                                          "✎"
                                        ),
                              React.createElement(
                                          "div",
                                          null,
                                          React.createElement(
                                                        "h3",
                                                        null,
                                                        "작성한 리뷰"
                                                      ),
                                          React.createElement(
                                                        "p",
                                                        null,
                                                        "내가 작성한 영화 리뷰를 확인할 수 있어요."
                                                      )
                                        ),
                              React.createElement(
                                          "span",
                                          {"className":"profile-coming"},
                                          "준비 중"
                                        )
                            ),
                    React.createElement(
                              "article",
                              {"className":"profile-activity-item"},
                              React.createElement(
                                          "span",
                                          {"className":"profile-activity-icon"},
                                          "♥"
                                        ),
                              React.createElement(
                                          "div",
                                          null,
                                          React.createElement(
                                                        "h3",
                                                        null,
                                                        "좋아요한 리뷰"
                                                      ),
                                          React.createElement(
                                                        "p",
                                                        null,
                                                        "마음에 들었던 리뷰를 모아볼 수 있어요."
                                                      )
                                        ),
                              React.createElement(
                                          "span",
                                          {"className":"profile-coming"},
                                          "준비 중"
                                        )
                            ),
                    React.createElement(
                              Link,
                              {"to":"/profile/favorites","className":"profile-activity-item profile-favorites-link"},
                              React.createElement(
                                          "span",
                                          {"className":"profile-activity-icon"},
                                          "★"
                                        ),
                              React.createElement(
                                          "div",
                                          null,
                                          React.createElement(
                                                        "h3",
                                                        null,
                                                        "관심 영화"
                                                      ),
                                          React.createElement(
                                                        "p",
                                                        null,
                                                        "관심 있는 영화를 관리할 수 있어요."
                                                      )
                                        ),
                              React.createElement(
                                          "span",
                                          {"className":"profile-favorites-count"},
                                          (favoriteCount),
                                          "개 보기 →"
                                        )
                            )
                  )
          ),
      React.createElement(
            "section",
            {"className":"profile-card profile-delete-section"},
            React.createElement(
                    "div",
                    null,
                    React.createElement(
                              "h2",
                              null,
                              "회원 탈퇴"
                            ),
                    React.createElement(
                              "p",
                              null,
                              "회원 탈퇴를 진행하면 계정 이용에 제한이 생길 수 있습니다."
                            )
                  ),
            React.createElement(
                    Link,
                    {"to":"/profile/delete","className":"profile-button profile-delete-button","style":{ textDecoration: "none" }},
                    "회원 탈퇴"
                  )
          )
    )
)
  );
}

export default Profile;
