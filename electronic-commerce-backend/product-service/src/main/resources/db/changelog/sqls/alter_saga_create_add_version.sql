ALTER TABLE saga_product_entities
    ADD COLUMN IF NOT EXISTS version BIGINT;