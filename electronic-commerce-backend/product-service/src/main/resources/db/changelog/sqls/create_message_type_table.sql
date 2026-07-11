CREATE TABLE IF NOT EXISTS messages (
    id varchar(256) PRIMARY KEY ,
    message_type varchar(32),
    message_created_time TIMESTAMP
)