CREATE TABLE car_reservation_customers
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(50) NOT NULL,
    phone         VARCHAR(20) NOT NULL,
    identity_hash VARCHAR(64) NOT NULL,

    INDEX idx_car_reservation_customers_identity_hash (identity_hash)
);
