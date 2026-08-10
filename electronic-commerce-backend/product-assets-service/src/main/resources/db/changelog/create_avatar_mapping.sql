CREATE TABLE product_avatar_mapping (
    product_id BIGINT PRIMARY KEY REFERENCES product_assets(id),
    avatar_id BIGINT UNIQUE REFERENCES media_content(id)
);