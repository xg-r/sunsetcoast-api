-- 떠나요 원본 파일(AdjustCalendar)을 캠핑장 정산 계정에 붙이도록 사업자번호를 넣어 둔다.
-- V38은 컬럼만 추가해서, 새 DB에서는 이 파일이 '사업자 278-27-01574'라는 새 계정으로 잡혔다.
-- 이미 API로 지정했거나 다른 계정이 같은 번호를 쓰고 있으면 건드리지 않는다.
UPDATE settlement_accounts
SET business_number = '2782701574'
WHERE source = 'DDNAYO'
  AND account = '서귀포 선셋코스트 카라반&캠핑장'
  AND business_number IS NULL
  AND NOT EXISTS (SELECT 1
                  FROM (SELECT id FROM settlement_accounts WHERE source = 'DDNAYO' AND business_number = '2782701574') taken);
