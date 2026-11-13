-- 직접 예약 입력 (RESERVATION-01)
--
-- 전화·현장 예약은 플랫폼 파일에 나오지 않으므로 관리자가 입력한다. reservations.source = 'DIRECT'.
-- 금액은 OTA 예약과 같은 방식으로 settlement_lines에 1박·객실마다 행을 만든다 (수수료 0, 업로드 배치 없음).
-- 외부 소유주 객실(Villa II·III) 전화 예약은 소유주 계좌로 입금되므로 payment_receiver = 'OWNER'.
-- 연락처는 암호화 저장이 필요해(PRIVACY-01) 아직 받지 않는다.

ALTER TABLE reservations
    ADD COLUMN guest_count    INT           NULL,
    ADD COLUMN payment_method VARCHAR(20)   NULL,
    ADD COLUMN paid           BOOLEAN       NULL,
    ADD COLUMN memo           VARCHAR(1000) NULL;

ALTER TABLE settlement_lines
    MODIFY import_batch_id BIGINT NULL;
