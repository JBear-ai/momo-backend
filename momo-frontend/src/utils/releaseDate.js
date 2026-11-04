export function formatReleaseDate(releaseDate) {
  const value = String(releaseDate || "").trim();

  if (!value) {
    return "개봉일 정보 없음";
  }

  if (!/^\d{8}$/.test(value)) {
    return "개봉일 확인 필요";
  }

  const year = Number(value.slice(0, 4));
  const month = Number(value.slice(4, 6));
  const day = Number(value.slice(6, 8));
  const date = new Date(year, month - 1, day);

  const isValidDate =
    date.getFullYear() === year &&
    date.getMonth() === month - 1 &&
    date.getDate() === day;

  if (!isValidDate) {
    return "개봉일 확인 필요";
  }

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  if (date > today) {
    return "개봉 예정";
  }

  return `${value.slice(0, 4)}.${value.slice(4, 6)}.${value.slice(6, 8)}`;
}
