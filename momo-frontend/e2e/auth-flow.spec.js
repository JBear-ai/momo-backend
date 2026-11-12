const { test, expect } = require("@playwright/test");

test("회원가입, 로그인, 로그아웃이 브라우저에서 이어진다", async ({ page }) => {
  page.on("dialog", (dialog) => dialog.accept());

  const suffix = Date.now().toString(36).slice(-7);
  const loginId = `e2e${suffix}`;
  const nickname = `M${suffix}`;
  const email = `${loginId}@example.com`;
  const password = "MomoE2e1!";

  await page.goto("/signup");

  await page.locator("#signupId").fill(loginId);
  await expect(page.getByText("사용 가능한 아이디입니다.")).toBeVisible();

  await page.locator("#signupEmail").fill(email);
  await page.locator("#signupPassword").fill(password);
  await page.locator("#signupPasswordCheck").fill(password);
  await page.locator("#signupNickname").fill(nickname);

  const signupButton = page.getByRole("button", {
    name: "무료 회원가입하고 영화 메모하러 가기",
  });
  await expect(signupButton).toBeEnabled();
  await signupButton.click();
  await expect(page).toHaveURL(/\/login$/);

  await page.locator("#loginId").fill(loginId);
  await page.locator("#loginPassword").fill(password);

  const initialFavoritesSync = page.waitForResponse((response) =>
    response.url().endsWith("/api/favorites") &&
    response.request().method() === "GET" &&
    response.ok(),
  );
  await page.getByRole("button", { name: "로그인" }).click();

  await expect(page).toHaveURL("http://127.0.0.1:3000/");
  await expect.poll(() => page.evaluate(() => localStorage.getItem("accessToken")))
    .not.toBeNull();
  await expect(page.getByRole("button", { name: "로그아웃" })).toBeVisible();
  await initialFavoritesSync;

  // 첫 로그인 동기화 후 서버에 관심 영화를 추가해 계정별 저장을 확인한다.
  const favoriteStatus = await page.evaluate(async (movie) => {
    const response = await fetch("http://127.0.0.1:8111/api/favorites", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${localStorage.getItem("accessToken")}`,
      },
      body: JSON.stringify(movie),
    });
    return response.status;
  }, {
    movieCode: "20261234",
    title: "관심 영화 E2E 테스트",
    releaseDate: "20261112",
    posterUrl: "",
  });
  expect(favoriteStatus).toBe(201);

  // 새로고침 후 서버의 관심 영화 목록이 화면에 복원되는지 확인한다.
  const refreshedFavoritesSync = page.waitForResponse((response) =>
    response.url().endsWith("/api/favorites") &&
    response.request().method() === "GET" &&
    response.ok(),
  );
  await page.reload();
  await refreshedFavoritesSync;
  await page.goto("/profile/favorites");
  await expect(page.getByText("관심 영화 E2E 테스트")).toBeVisible();

  // 관심 영화 화면의 버튼으로 서버 목록에서 삭제하고 빈 상태까지 반영되는지 확인한다.
  const favoriteDelete = page.waitForResponse((response) =>
    response.url().endsWith("/api/favorites/20261234") &&
    response.request().method() === "DELETE" &&
    response.ok(),
  );
  await page.getByRole("button", { name: "관심 영화 해제" }).click();
  await favoriteDelete;
  await expect(page.getByText("아직 등록한 관심 영화가 없습니다.")).toBeVisible();

  await page.getByRole("button", { name: "로그아웃" }).click();
  await expect(page.getByRole("link", { name: "로그인" })).toBeVisible();
  await expect.poll(() => page.evaluate(() => localStorage.getItem("accessToken")))
    .toBeNull();
  await expect(page.getByRole("button", { name: "로그아웃" })).toHaveCount(0);
});
