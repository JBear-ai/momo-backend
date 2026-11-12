const { test, expect } = require("@playwright/test");

test("회원가입부터 로그인, 마이페이지, 로그아웃까지 실제 서버 흐름을 완료한다", async ({ page }) => {
  page.on("dialog", (dialog) => dialog.accept());

  const suffix = Date.now().toString(36).slice(-7);
  const loginId = `e2e${suffix}`;
  const nickname = `M${suffix}`;
  const email = `${loginId}@example.com`;
  const password = "MomoE2e1!";

  await page.goto("/signup");

  await page.getByLabel("아이디").fill(loginId);
  await expect(page.getByText("사용 가능한 아이디입니다.")).toBeVisible();

  await page.getByLabel("이메일").fill(email);
  await page.getByLabel("비밀번호", { exact: true }).fill(password);
  await page.getByLabel("비밀번호 확인").fill(password);
  await page.getByLabel("닉네임").fill(nickname);

  await page.getByRole("button", { name: /무료 회원가입/ }).click();
  await expect(page).toHaveURL(/\/login$/);

  await page.getByLabel("아이디").fill(loginId);
  await page.getByLabel("비밀번호").fill(password);
  await page.getByRole("button", { name: /로그인/ }).click();

  await expect(page.getByText(`${nickname}님`)).toBeVisible();
  await page.getByRole("link", { name: "마이페이지" }).click();
  await expect(page.getByRole("heading", { name: "마이페이지" })).toBeVisible();

  await page.getByRole("button", { name: /로그아웃/ }).click();
  await expect(page.getByRole("link", { name: "로그인" })).toBeVisible();
  await expect(page.getByRole("link", { name: "마이페이지" })).toHaveCount(0);
});
