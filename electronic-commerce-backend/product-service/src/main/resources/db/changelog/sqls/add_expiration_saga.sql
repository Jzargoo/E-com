ALTER TABLE saga_product_entities ADD COLUMN IF NOT EXISTS expiration_date TIMESTAMP;
ALTER TABLE saga_product_entities ADD COLUMN IF NOT EXISTS status VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_expiration_date ON saga_product_entities (expiration_date);