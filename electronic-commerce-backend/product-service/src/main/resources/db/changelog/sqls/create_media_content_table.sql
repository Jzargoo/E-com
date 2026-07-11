CREATE TABLE
    IF NOT EXISTS
        media_content(
            media_content varchar(256) NOT NULL ,
            product_id bigint REFERENCES products(id),
            PRIMARY KEY (product_id, media_content)
        )