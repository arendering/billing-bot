--liquibase formatted sql

-- changeset liquibase:001
CREATE TABLE users(
    telegram_id   BIGINT        NOT NULL,
    user_id       BIGINT        NOT NULL,
    agreement_id  BIGINT        NOT NULL,
    login         VARCHAR(255)  NOT NULL,
    PRIMARY KEY (telegram_id)
);

--changeset liquibase:002
CREATE TABLE payment_notification_messages(
    telegram_id  BIGINT NOT NULL,
    message_id   INT    NOT NULL,
    PRIMARY KEY (telegram_id)
);

--changeset liquibase:003
CREATE TABLE payment_notifications(
    telegram_id        BIGINT       NOT NULL,
    notification_type  VARCHAR(255) NOT NULL,
    PRIMARY KEY (telegram_id)
);
