-- 리조트 프리미엄은 205호. 요금표의 139,000원은 성수기 요금, 비수기는 105,000원 (주중·주말 같음)
UPDATE room_list_prices
SET offpeak_weekday = 105000,
    offpeak_weekend = 105000,
    peak_weekday    = 139000,
    peak_weekend    = 139000
WHERE room_id = (SELECT id FROM rooms WHERE name = 'RESORT_205');
