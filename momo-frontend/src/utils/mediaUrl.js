const API_BASE_URL = (
  process.env.REACT_APP_API_BASE_URL || "http://localhost:8111"
).replace(/\/+$/, "");

export function resolvePosterUrl(value) {
  if (typeof value !== "string" || !value.trim()) {
    return "";
  }

  const url = value.trim();
  if (/^(https?:)?\/\//i.test(url) || /^(data|blob):/i.test(url)) {
    return url;
  }

  // 로컬에 저장된 포스터 경로만 백엔드 주소를 붙인다.
  if (url.startsWith("/posters/")) {
    return `${API_BASE_URL}${url}`;
  }

  return url;
}
