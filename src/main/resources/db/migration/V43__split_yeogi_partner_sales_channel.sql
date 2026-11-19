-- 여기어때 정산 파일의 판매채널 '여기어때 연동 예약 사이트'를 여기어때(YEOGI)와 나눈다 (YEOGI_PARTNER).
-- 지금까지 올린 예약은 원본 판매채널(sales_channel_raw)로 다시 분류한다.
UPDATE reservations
SET sales_channel = 'YEOGI_PARTNER'
WHERE sales_channel = 'YEOGI'
  AND sales_channel_raw LIKE '%여기어때%'
  AND sales_channel_raw LIKE '%연동%';
