-- 관리비와 함께 현장에 넘겨 줄 추가요금 (바베큐, 추가인원 등).
-- 손님이 현장에 낼 추가요금을 숙박비와 함께 우리 계좌로 보낸 경우, 관리비를 지급할 때 같이 이체한다.
-- 관리비 행(예약 × 객실)마다 여러 건. 금액 = 단가 × 수량 × 박수(nights가 NULL이면 1).
CREATE TABLE management_fee_extras
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    management_fee_id BIGINT         NOT NULL,
    -- BBQ: 바베큐, EXTRA_PERSON: 추가인원 (1박 1인당), OTHER: 기타
    item              VARCHAR(20)    NOT NULL,
    unit_price        DECIMAL(15, 2) NOT NULL,
    quantity          INT            NOT NULL,
    -- 추가인원처럼 1박마다 받는 요금의 박수
    nights            INT,
    amount            DECIMAL(15, 2) NOT NULL,
    memo              VARCHAR(255),
    created_at        DATETIME       NOT NULL,
    CONSTRAINT fk_management_fee_extras_fee FOREIGN KEY (management_fee_id) REFERENCES management_fees (id) ON DELETE CASCADE
);

CREATE INDEX idx_management_fee_extras_fee ON management_fee_extras (management_fee_id);
