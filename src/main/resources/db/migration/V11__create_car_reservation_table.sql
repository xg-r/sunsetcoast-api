CREATE TABLE car_reservations
(
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    car_id                      BIGINT NOT NULL,
    car_reservation_customer_id BIGINT NOT NULL,
    start_at                    DATETIME NOT NULL,
    end_at                      DATETIME NOT NULL,
    memo                        VARCHAR(500),

    INDEX idx_car_reservations_car_period (car_id, start_at, end_at)
);
