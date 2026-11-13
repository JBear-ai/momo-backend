import { getPosterPollDelay } from "./posterPolling";

describe("getPosterPollDelay", () => {
  test("재시도 횟수에 따라 대기 시간을 늘린다", () => {
    expect(getPosterPollDelay(0)).toBe(4000);
    expect(getPosterPollDelay(1)).toBe(8000);
    expect(getPosterPollDelay(2)).toBe(16000);
  });

  test("최대 대기 시간을 넘지 않는다", () => {
    expect(getPosterPollDelay(10)).toBe(60000);
    expect(getPosterPollDelay(10, 1500, 15000)).toBe(15000);
  });

  test("음수 재시도 횟수는 최초 대기 시간으로 처리한다", () => {
    expect(getPosterPollDelay(-1)).toBe(4000);
  });
});
