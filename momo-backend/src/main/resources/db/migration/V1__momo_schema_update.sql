
-- MOMO V1
-- 기존 리뷰 테이블의 사용하지 않는 컬럼 정리
-- 기존 리뷰 데이터 보존

-- 1. reviews.rating 컬럼이 존재하면 제거
SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'reviews'
      AND COLUMN_NAME = 'rating'
);

SET @sql = IF(
    @column_exists > 0,
    'ALTER TABLE reviews DROP COLUMN rating',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. reviews.liked 컬럼이 존재하면 제거
SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'reviews'
      AND COLUMN_NAME = 'liked'
);

SET @sql = IF(
    @column_exists > 0,
    'ALTER TABLE reviews DROP COLUMN liked',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
