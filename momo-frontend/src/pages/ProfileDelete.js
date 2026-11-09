import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import AxiosApi from "../api/AxiosApi";

function ProfileDelete() {
  const navigate = useNavigate();
  const [password, setPassword] = useState("");
  const [isConfirmed, setIsConfirmed] = useState(false);

  const handleDelete = async () => {
    // 회원 탈퇴 처리
    // 비밀번호 입력 확인
    if (!password.trim()) {
      alert("비밀번호를 입력해 주세요.");
      return;
    }

    // 회원 탈퇴 동의 확인
    if (!isConfirmed) {
      alert("회원 탈퇴 안내를 확인해 주세요.");
      return;
    }

    // 최종 탈퇴 확인
    const confirmed = window.confirm("정말 회원 탈퇴하시겠습니까?");

    if (!confirmed) {
      return;
    }

    try {
      // Spring Boot 회원 탈퇴 API 호출
      await AxiosApi.memberDelete(password);

      // 탈퇴 성공 후 브라우저에 저장된 로그인 정보 제거
      localStorage.removeItem("username");
      localStorage.removeItem("nickname");
      localStorage.removeItem("email");
      localStorage.removeItem("password");

      alert("회원 탈퇴가 완료되었습니다.");

      // 로그인 화면으로 이동
      navigate("/login", { replace: true });
    } catch (error) {
      console.error("회원 탈퇴 실패:", error);

      alert(
        error.response?.data?.message ||
          (typeof error.response?.data === "string"
            ? error.response.data
            : "회원 탈퇴 중 오류가 발생했습니다."),
      );
    }
  };
  return (
    React.createElement(
  "main",
  {"className":"profile-page"},
  React.createElement(
      "section",
      {"className":"profile-container"},
      React.createElement(
            "header",
            {"className":"profile-heading"},
            React.createElement(
                    "h1",
                    null,
                    "회원 탈퇴"
                  ),
            React.createElement(
                    "p",
                    null,
                    "탈퇴하기 전에 안내 사항을 확인해 주세요."
                  )
          ),
      React.createElement(
            "section",
            {"className":"profile-card profile-delete-page-card"},
            React.createElement(
                    "h2",
                    null,
                    "정말 탈퇴하시겠습니까?"
                  ),
            React.createElement(
                    "p",
                    null,
                    "계정을 삭제하려면 본인 확인을 위해 현재 비밀번호를 입력해 주세요."
                  ),
            React.createElement(
                    "div",
                    {"style":{ margin: "20px 0" }},
                    React.createElement(
                              "label",
                              {"htmlFor":"delete-password","style":{
                                              display: "block",
                                              marginBottom: "8px",
                                              fontWeight: "bold",
                                            }},
                              "현재 비밀번호"
                            ),
                    React.createElement(
                              "input",
                              {"id":"delete-password","type":"password","value":password,"onChange":(event) => setPassword(event.target.value),"placeholder":"비밀번호를 입력해 주세요.","style":{
                                              width: "100%",
                                              padding: "10px",
                                              boxSizing: "border-box",
                                            }}
                            )
                  ),
            React.createElement(
                    "label",
                    {"className":"profile-delete-check"},
                    React.createElement(
                              "input",
                              {"type":"checkbox","checked":isConfirmed,"onChange":(event) => setIsConfirmed(event.target.checked)}
                            ),
                    "회원 탈퇴 안내를 확인했습니다."
                  ),
            React.createElement(
                    "div",
                    {"className":"profile-form-buttons","style":{ marginTop: "20px" }},
                    React.createElement(
                              "button",
                              {"type":"button","className":"profile-button profile-delete-button","onClick":handleDelete,"disabled":!isConfirmed},
                              "탈퇴하기"
                            ),
                    React.createElement(
                              "button",
                              {"type":"button","className":"profile-button profile-button-secondary","onClick":() => navigate("/profile")},
                              "취소"
                            )
                  )
          )
    )
)
  );
}

export default ProfileDelete;
