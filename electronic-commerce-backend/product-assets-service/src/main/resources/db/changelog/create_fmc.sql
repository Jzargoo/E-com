CREATE TABLE IF NOT EXISTS fallback_media_content (
    queue_id BIGINT PRIMARY KEY ,
    media_uri VARCHAR(128) NOT NULL UNIQUE ,
    product_id BIGINT NOT NULL REFERENCES product_assets(id),
    is_avatar BOOLEAN NOT NULL DEFAULT FALSE,
    previous_uri VARCHAR(128) NOT NULL ,
    is_free BOOLEAN NOT NULL DEFAULT TRUE,
    previous_media_version VARCHAR(64) NOT NULL
)