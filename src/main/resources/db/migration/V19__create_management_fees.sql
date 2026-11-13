-- 현장 관리비 (SETTLE-02)
--
-- 현장 사장님에게 예약의 객실마다 1회 지급한다. 박수와 무관하다.
-- 연박은 예약번호로만 판단한다 (예약번호가 다르면 이어서 묵어도 따로 지급).
-- 취소 예약은 0원. 관리자가 예약별로 금액을 고칠 수 있다 (원래 금액은 calculated_amount에 남는다).

-- 객실별 관리비 단가. 입실일 기준으로 적용 기간이 맞는 단가를 쓴다.
CREATE TABLE management_fee_rates
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id        BIGINT         NOT NULL,
    amount         DECIMAL(15, 2) NOT NULL,
    effective_from DATE           NOT NULL,
    -- 이 날짜부터는 적용하지 않는다. NULL이면 현재 단가
    effective_to   DATE,
    CONSTRAINT uq_management_fee_rates_room_from UNIQUE (room_id, effective_from),
    CONSTRAINT fk_management_fee_rates_room FOREIGN KEY (room_id) REFERENCES rooms (id)
);

-- Villa II, III는 우리가 지급하지 않는다 (현장에서 소유주에게 직접 청구) → 0원
INSERT INTO management_fee_rates (room_id, amount, effective_from)
SELECT id,
       CASE
           WHEN name IN ('바다', '노을', '해안', '바람') THEN 40000
           WHEN type IN ('SPACE_CARAVAN', 'AIR_STREAM', 'CARAVAN') THEN 45000
           WHEN type = 'RESORT' THEN 30000
           WHEN name IN ('PREMIUM_VILLA', 'VILLA_1') THEN 130000
           WHEN name IN ('VILLA_2', 'VILLA_3') THEN 0
           END,
       '2020-01-01'
FROM rooms;

-- (예약, 객실)마다 1행. 다중 객실 예약이 들어오면 객실마다 행이 생긴다.
CREATE TABLE management_fees
(
    id                           BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id               BIGINT         NOT NULL,
    room_id                      BIGINT         NOT NULL,
    -- CHARGED: 지급, CANCELLED: 취소 예약, NO_STAY: 투숙한 밤 없음, NO_RATE: 단가 없음
    basis                        VARCHAR(20)    NOT NULL,
    calculated_amount            DECIMAL(15, 2) NOT NULL,
    -- 관리자 수정. NULL이면 calculated_amount를 쓴다. 재계산이 덮어쓰지 않는다.
    override_amount              DECIMAL(15, 2),
    override_reason              VARCHAR(255),
    overridden_at                DATETIME,
    created_at                   DATETIME       NOT NULL,
    updated_at                   DATETIME       NOT NULL,
    CONSTRAINT uq_management_fees_reservation_room UNIQUE (reservation_id, room_id),
    CONSTRAINT fk_management_fees_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT fk_management_fees_room FOREIGN KEY (room_id) REFERENCES rooms (id)
);
