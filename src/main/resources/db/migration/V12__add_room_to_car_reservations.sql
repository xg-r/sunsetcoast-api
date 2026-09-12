ALTER TABLE car_reservations
    ADD COLUMN room_id BIGINT NOT NULL;

CREATE INDEX idx_car_reservations_room_id
    ON car_reservations (room_id);
