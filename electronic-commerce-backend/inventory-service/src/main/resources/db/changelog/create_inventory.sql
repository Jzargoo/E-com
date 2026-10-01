CREATE TABLE IF NOT EXISTS inventory (
    product_id BIGINT PRIMARY KEY ,
    quantity INT NOT NULL,
    version INT NOT NULL,
    shop_id INT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);