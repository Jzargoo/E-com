CREATE TABLE messages(
    message_id varchar(256) PRIMARY KEY ,
    processed_at TIMESTAMP NOT NULL ,
    message_type varchar(64) NOT NULL
)