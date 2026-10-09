CREATE TABLE IF NOT EXISTS reservation (
    order_id BIGINT PRIMARY KEY,
    quantity INT NOT NULL,
    inventory_id BIGINT REFERENCES inventory(product_id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    customer_id BIGINT NOT NULL
);
