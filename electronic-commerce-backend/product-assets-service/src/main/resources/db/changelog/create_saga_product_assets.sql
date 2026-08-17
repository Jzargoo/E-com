CREATE TABLE saga_product_assets (
    product_id BIGINT PRIMARY KEY REFERENCES product_assets(id),
    shop_id INT UNIQUE,
    error_message VARCHAR(128)
)