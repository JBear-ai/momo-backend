import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "../App.css";
import AxiosApi from "../api/AxiosApi";

function ProfileDelete() {
  const navigate = useNavigate();
  const [password, setPassword] = useState("");
  const [isConfirmed, setIsConfirmed] = useState(false);

  // const handleDelete = () => { 기존 부분 주석 처리
  //   if (!password.trim()) {
  //     alert("비밀번호를 입력해 주세요.");
  //     return;
  //   }

  //   // 로컬스토리지에 저장된 비밀번호 확인 (기본값 "1234" 또는 실제 저장된 비번)
  //   const savedPassword = localStorage.getItem("password") || "1234";

  //   if (password !== savedPassword) {
  //     alert("비밀번호가 일치하지 않습니다.");
  //     return;
  //   }

  //   if (!isConfirmed) {
  //     alert("회원 탈퇴 안내를 확인해 주세요.");
  //     return;
  //   }

  //   // 비밀번호가 일치하고 체크박스도 체크된 경우
  //   const confirmed = window.confirm("정말 회원 탈퇴하시겠습니까?");
  //   if (!confirmed) {
  //     return;
  //   }

  //   // 실제 백엔드 연동 시 탈퇴 API 호출 및 로컬스토리지 초기화 등 처리
  //   // localStorage.clear();

  //   alert("회원 탈퇴 API가 아직 연결되지 않았습니다.");
  // };

  const handleDelete = async () => {
    // 회원 탈퇴 처리 / 11.02
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
    <main className="profile-page">
      <section className="profile-container">
        <header className="profile-heading">
          <h1>회원 탈퇴</h1>
          <p>탈퇴하기 전에 안내 사항을 확인해 주세요.</p>
        </header>

        <section className="profile-card profile-delete-page-card">
          <h2>정말 탈퇴하시겠습니까?</h2>
          <p>
            계정을 삭제하려면 본인 확인을 위해 현재 비밀번호를 입력해 주세요.
          </p>

          {/* 비밀번호 확인 입력 필드 추가 */}
          <div style={{ margin: "20px 0" }}>
            <label
              htmlFor="delete-password"
              style={{
                display: "block",
                marginBottom: "8px",
                fontWeight: "bold",
              }}
            >
              현재 비밀번호
            </label>
            <input
              id="delete-password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="비밀번호를 입력해 주세요."
              style={{
                width: "100%",
                padding: "10px",
                boxSizing: "border-box",
              }}
            />
          </div>

          <label className="profile-delete-check">
            <input
              type="checkbox"
              checked={isConfirmed}
              onChange={(event) => setIsConfirmed(event.target.checked)}
            />
            회원 탈퇴 안내를 확인했습니다.
          </label>

          <div className="profile-form-buttons" style={{ marginTop: "20px" }}>
            <button
              type="button"
              className="profile-button profile-delete-button"
              onClick={handleDelete}
              disabled={!isConfirmed}
            >
              탈퇴하기
            </button>

            <button
              type="button"
              className="profile-button profile-button-secondary"
              onClick={() => navigate("/profile")}
            >
              취소
            </button>
          </div>
        </section>
      </section>
    </main>
  );
}

export default ProfileDelete;
