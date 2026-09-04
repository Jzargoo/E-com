ALTER TABLE saga_product_entities ADD COLUMN IF NOT EXISTS error_message varchar(128);
ALTER TABLE saga_product_entities ADD COLUMN IF NOT EXISTS stock DECIMAL(10, 2);