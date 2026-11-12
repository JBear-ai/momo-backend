const fs = require("fs");
const path = require("path");

const layoutSource = fs.readFileSync(path.join(__dirname, "Layout.js"), "utf8");

describe("푸터 리뷰 바로가기", () => {
  test("작성한 리뷰와 좋아요한 리뷰의 기존 페이지 경로를 사용한다", () => {
    expect(layoutSource).toContain(
      '<Link to="/profile/reviews">작성한 리뷰</Link>',
    );
    expect(layoutSource).toContain(
      '<Link to="/profile/liked-reviews">좋아요한 리뷰</Link>',
    );
  });

  test("회원 활동 바로가기는 로그인 회원에게만 표시한다", () => {
    const activitySection = layoutSource.slice(
      layoutSource.indexOf("{nickname && ("),
    );

    expect(activitySection).toContain("<FooterColumn>");
    expect(activitySection).toContain("</FooterColumn>");
  });
});
