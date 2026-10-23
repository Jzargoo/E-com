CREATE TABLE product_assets(
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    shop_id INT NOT NULL
);