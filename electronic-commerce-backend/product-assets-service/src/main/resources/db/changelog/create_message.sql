CREATE TABLE messages(
    id BIGSERIAL PRIMARY KEY ,
    message_id varchar(256) ,
    processed_at TIMESTAMP NOT NULL ,
    message_type varchar(64) NOT NULL
)