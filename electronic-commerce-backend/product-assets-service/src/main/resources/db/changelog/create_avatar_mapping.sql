CREATE TABLE product_avatar_mapping (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT REFERENCES product_assets(id),
    content_id BIGINT UNIQUE REFERENCES media_content(id)
);