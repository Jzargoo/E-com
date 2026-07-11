CREATE TABLE IF NOT EXISTS  saga_product_entities(
    id bigint PRIMARY KEY REFERENCES products(id),
    step varchar(256)
)