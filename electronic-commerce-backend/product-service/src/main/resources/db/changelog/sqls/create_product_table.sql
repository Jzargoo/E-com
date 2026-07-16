CREATE TABLE
    IF NOT EXISTS
    products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64),
    description VARCHAR(512),
    shop_id INTEGER,
    characteristics jsonb,
    status varchar(32) NOT NULL,
    stock_price DECIMAL(10, 2),
    category_id INT REFERENCES categories
)