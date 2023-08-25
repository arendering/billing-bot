--liquibase formatted sql

-- changeset liquibase:001
CREATE TABLE users(
    telegram_id                   BIGINT        NOT NULL,
    uid                           BIGINT        NOT NULL,
    agrm_id                       BIGINT        NOT NULL,
    login                         VARCHAR(255)  NOT NULL,
    password                      VARCHAR(255)  NOT NULL,
    payment_notification_enabled  BOOLEAN       NOT NULL,
    PRIMARY KEY (telegram_id)
);

--changeset liquibase:002
CREATE TABLE payment_notification_messages(
    telegram_id BIGINT NOT NULL,
    message_id  INT    NOT NULL,
    PRIMARY KEY (telegram_id)
);
