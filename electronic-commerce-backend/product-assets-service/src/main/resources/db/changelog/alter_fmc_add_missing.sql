ALTER TABLE fallback_media_content
ADD COLUMN IF NOT EXISTS content_type VARCHAR(64) NOT NULL;