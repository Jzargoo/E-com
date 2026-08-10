CREATE TABLE media_content(
    queue_id BIGSERIAL PRIMARY KEY ,
    uri VARCHAR(128) NOT NULL UNIQUE ,
    product_id BIGINT NOT NULL REFERENCES product_assets(id),
    media_version INT NOT NULL DEFAULT 1
)