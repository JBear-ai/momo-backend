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

test("인물 ID와 필모그래피 영화 ID가 통합 영화 상세로 정확히 연결된다", async ({ page }) => {
  await page.route("**/api/persons/by-id/director/321", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        personInfo: {
          tmdbId: 321,
          name: "인물 연결 E2E",
          profileUrl: "",
          birthday: null,
          deathday: null,
          placeOfBirth: null,
          biography: "인물 ID 연결 검증",
          knownForDepartment: "Directing",
        },
        filmography: [
          {
            tmdbId: 888,
            title: "필모그래피 이동 E2E",
            releaseDate: "2026-11-12",
            posterUrl: "",
            role: "Director",
          },
        ],
      }),
    }),
  );

  await page.route("**/api/persons/filmography/888", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ movieCode: "20261234" }),
    }),
  );

  await page.route("**/api/movies/resolve/20261234", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        tmdbId: 888,
        movieCode: "20261234",
        title: "필모그래피 이동 E2E",
        titleEn: "Filmography Navigation E2E",
        releaseDate: "20261112",
        runningTime: "100",
        genre: "드라마",
        director: "인물 연결 E2E",
        actors: [],
        watchGrade: "전체 관람가",
        overview: "영화 상세 이동 확인",
        posterUrl: "",
        people: [
          {
            personId: 321,
            name: "인물 연결 E2E",
            originalName: "Person Navigation E2E",
            profileUrl: "",
            department: "director",
          },
        ],
      }),
    }),
  );

  await page.route("**/api/movies/20261234/poster", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ movieCode: "20261234", posterUrl: "" }),
    }),
  );

  await page.route("**/api/reviews/movie/20261234**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ content: [], totalElements: 0, totalPages: 0 }),
    }),
  );

  await page.route("**/api/movies/20261234/rating", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        likeCount: 0,
        dislikeCount: 0,
        totalCount: 0,
        positiveRate: null,
        myRating: null,
      }),
    }),
  );

  await page.goto("/person/director/321");
  await expect(
    page.getByRole("heading", { level: 1, name: "인물 연결 E2E" }),
  ).toBeVisible();

  await page.getByRole("button", {
    name: "필모그래피 이동 E2E 영화 상세정보 보기",
  }).click();

  await expect(page).toHaveURL(/\/movie\/20261234$/);
  await expect(
    page.getByRole("heading", { level: 1, name: "필모그래피 이동 E2E" }),
  ).toBeVisible();

  // 영화 상세의 TMDb 인물 ID가 이름 검색이 아닌 고유 ID 경로로 연결되는지 확인한다.
  await page.getByRole("button", { name: "인물 연결 E2E" }).click();
  await expect(page).toHaveURL(/\/person\/director\/321$/);
  await expect(
    page.getByRole("heading", { level: 1, name: "인물 연결 E2E" }),
  ).toBeVisible();
});
