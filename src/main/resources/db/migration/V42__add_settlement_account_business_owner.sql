-- NOL 정산 계정의 사업자(객실 명의).
-- NOL에서 받은 그대로의 정산 파일(`정산상세내역_yyyyMMdd.xls`)은 파일명에 판매자 계정이 없고, 파일 안에도 업소명뿐이다.
-- NOL은 객실 명의 사업자별로 정산하므로, 파일 예약의 객실 명의로 이 계정을 찾는다.
ALTER TABLE settlement_accounts
    ADD COLUMN business_owner_id BIGINT NULL AFTER business_number,
    ADD CONSTRAINT fk_settlement_accounts_business_owner FOREIGN KEY (business_owner_id) REFERENCES business_owners (id);

-- V24에서 넣은 NOL 계정 ↔ V4·V14의 사업자. 이미 지정했으면 건드리지 않는다
UPDATE settlement_accounts sa
    JOIN business_owners bo ON bo.name = CASE sa.account
        WHEN '선셋코스트' THEN '선셋코스트'
        WHEN '대흥cns' THEN '대흥씨엔에스(주)'
        WHEN '스테이디앤디' THEN '스테이디앤디풀빌라'
        WHEN '어라운드대정' THEN '어라운드대정'
        WHEN '대정시크릿' THEN '대정시크릿'
        END
SET sa.business_owner_id = bo.id
WHERE sa.source = 'NOL'
  AND sa.business_owner_id IS NULL;
