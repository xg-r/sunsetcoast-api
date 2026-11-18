-- 현장 객실 변경. 손님이 현장에서 방을 옮기면(예: 해안 → 놀멍) 관리비는 옮긴 객실 단가로 주고,
-- 손님이 낸 차액(계좌이체)은 그 행의 매출에 더한다. 예약의 원래 객실(room_id)은 그대로 둔다.
-- moved_room_id: 실제로 묵은 객실. room_change_difference: 받은 차액 (환불이면 음수)
ALTER TABLE management_fees
    ADD COLUMN moved_room_id          BIGINT         NULL AFTER room_id,
    ADD COLUMN room_change_difference DECIMAL(15, 2) NULL AFTER moved_room_id,
    ADD COLUMN room_change_memo       VARCHAR(255)   NULL AFTER room_change_difference,
    ADD COLUMN room_changed_at        DATETIME       NULL AFTER room_change_memo,
    ADD CONSTRAINT fk_management_fees_moved_room FOREIGN KEY (moved_room_id) REFERENCES rooms (id);
