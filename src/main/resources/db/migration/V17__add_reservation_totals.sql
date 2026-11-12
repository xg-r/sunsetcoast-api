-- 예약 단위 합계 (해당 예약의 모든 정산 행 합). 정산 행이 들어올 때마다 다시 계산한다.
-- 여기어때/NOL은 1박마다 정산 행이 생기고 떠나요는 예약 1행이지만, 예약 테이블에서는 같은 형태로 본다.
ALTER TABLE reservations
    ADD COLUMN gross_amount      DECIMAL(15, 2) NOT NULL DEFAULT 0 AFTER nights,
    ADD COLUMN commission_amount DECIMAL(15, 2) NOT NULL DEFAULT 0 AFTER gross_amount,
    ADD COLUMN net_amount        DECIMAL(15, 2) NOT NULL DEFAULT 0 AFTER commission_amount;
