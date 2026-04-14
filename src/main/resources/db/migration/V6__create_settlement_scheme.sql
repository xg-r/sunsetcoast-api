CREATE TABLE settlements
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,

    reservation_number VARCHAR(50)    NOT NULL,
    platform           VARCHAR(30)    NOT NULL,

    room_id            BIGINT         NOT NULL,
    business_id        BIGINT         NOT NULL,

    reserved_at        DATE           NOT NULL,
    check_in_date      DATE           NOT NULL,
    nights             INT            NOT NULL,

    booker_name        VARCHAR(50),
    booker_phone       VARCHAR(20),

    inflow_channel     VARCHAR(50),
    payment_type       VARCHAR(50),
    reservation_status VARCHAR(50),
    settlement_status  VARCHAR(50),

    gross_amount       DECIMAL(15, 2) NOT NULL,
    commission_amount  DECIMAL(15, 2) NOT NULL,
    net_amount         DECIMAL(15, 2) NOT NULL,

    created_at         TIMESTAMP      NOT NULL,
    updated_at         TIMESTAMP      NOT NULL,

    CONSTRAINT uq_settlements_reservation_number_platform
        UNIQUE (reservation_number, platform)
);
