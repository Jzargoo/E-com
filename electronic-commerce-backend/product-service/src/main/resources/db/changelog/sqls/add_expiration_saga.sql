ALTER TABLE saga_product_entities ADD COLUMN expiration_date TIMESTAMP;
ALTER TABLE saga_product_entities ADD COLUMN status VARCHAR(64);

CREATE INDEX idx_expiration_date ON saga_product_entities (expiration_date);