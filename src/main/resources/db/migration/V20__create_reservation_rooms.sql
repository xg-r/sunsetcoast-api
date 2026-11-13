-- 다중 객실 예약
--
-- 떠나요는 한 예약에 객실 여러 개를 쉼표로 이어서 적는다 ("수애기 (오션뷰&돌고래뷰),스페이스카라반 쉬멍(오..").
-- 예약-객실을 N:M으로 두고, 정산 행은 객실마다 따로 만든다 (금액은 객실 정가 비율로 나눈 추정값).
-- reservations.room_id는 대표 객실(첫 번째 객실)로 남긴다.

CREATE TABLE reservation_rooms
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    room_id        BIGINT NOT NULL,
    -- 파일에 적힌 순서 (0부터)
    sort_order     INT    NOT NULL DEFAULT 0,
    CONSTRAINT uq_reservation_rooms UNIQUE (reservation_id, room_id),
    CONSTRAINT fk_reservation_rooms_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT fk_reservation_rooms_room FOREIGN KEY (room_id) REFERENCES rooms (id)
);

INSERT INTO reservation_rooms (reservation_id, room_id, sort_order)
SELECT id, room_id, 0
FROM reservations;

-- 정산 행에 객실을 넣고, 행 자연키에 객실을 추가한다.
ALTER TABLE settlement_lines
    ADD COLUMN room_id BIGINT NULL AFTER reservation_id;

UPDATE settlement_lines s JOIN reservations r ON r.id = s.reservation_id
SET s.room_id = r.room_id;

ALTER TABLE settlement_lines
    MODIFY room_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_settlement_lines_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    ADD CONSTRAINT uq_settlement_lines_room_key UNIQUE (reservation_id, room_id, line_type, stay_date, seq),
    DROP INDEX uq_settlement_lines_key;

-- 미매핑 행을 관리자가 객실을 지정해 처리한 시각. result는 처리 결과로 바뀐다.
ALTER TABLE import_rows
    ADD COLUMN resolved_at DATETIME NULL;
