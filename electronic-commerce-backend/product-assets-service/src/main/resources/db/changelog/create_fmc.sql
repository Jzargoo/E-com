CREATE TABLE fallback_media_content(
    queue_id BIGINT PRIMARY KEY ,
    media_uri VARCHAR(128) NOT NULL UNIQUE ,
    product_id BIGINT NOT NULL REFERENCES product_assets(id),
    is_avatar BOOLEAN NOT NULL DEFAULT FALSE,
    media_version INT NOT NULL,
)