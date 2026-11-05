-- FILE: momo-backend/src/main/resources/db/migration/V2__add_kobis_detail_retry_timestamp.sql — feature/api-cache-hardening
ALTER TABLE movies ADD COLUMN kobis_detail_attempted_at DATETIME NULL;