// 포스터 수집 상태를 확인할 때 재시도 간격을 점진적으로 늘린다.
export function getPosterPollDelay(attempt, baseDelay = 4000, maxDelay = 60000) {
  const safeAttempt = Math.max(0, Number(attempt) || 0);
  return Math.min(baseDelay * (2 ** safeAttempt), maxDelay);
}
