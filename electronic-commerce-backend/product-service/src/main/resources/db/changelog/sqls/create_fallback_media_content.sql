CREATE TABLE fallback_media_content (
    queue_id BIGSERIAL PRIMARY KEY ,
    content_type VARCHAR(128),
    media_id VARCHAR(1024),
    product_id BIGINT REFERENCES product(id)
)
