-- NOL 정산 파일에는 예약 일시가 없다. 예약번호 앞 6자리가 예약일(yyMMdd)이므로 날짜로 채운다.
-- (2026년 10월 NOL 예약 파일 59건의 '예약 일시'와 모두 일치. 시각은 알 수 없어 00:00)
UPDATE reservations
SET reserved_at = STR_TO_DATE(LEFT(reservation_number, 6), '%y%m%d')
WHERE source = 'NOL'
  AND reserved_at IS NULL
  AND reservation_number REGEXP '^[0-9]{16}$'
  AND STR_TO_DATE(LEFT(reservation_number, 6), '%y%m%d') IS NOT NULL;
