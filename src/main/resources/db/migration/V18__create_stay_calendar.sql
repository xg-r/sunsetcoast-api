-- 성수기/주말 판단 (머신러닝 피처용)
--
-- 주말: 금·토요일 밤 + 공휴일 전날 밤. 공휴일은 매년 이 테이블에 시드로 추가한다.
-- 성수기: 달력을 따로 관리하지 않는다. 예약 금액이 요금표(room_list_prices)와 정확히 일치하면
--         그 날짜에 성수기/비수기 한 표를 주고, 업소·날짜별 다수결로 정한다 (season_days).
-- 예약에는 박마다가 아니라 예약 단위로 저장한다: 주말 밤이 하나라도 있으면 주말 예약.

CREATE TABLE holidays
(
    holiday_date DATE        NOT NULL PRIMARY KEY,
    name         VARCHAR(50) NOT NULL
);

-- 공휴일 (대체공휴일, 임시공휴일, 선거일 포함). 2026년부터 노동절(5/1), 제헌절(7/17) 포함.
INSERT INTO holidays (holiday_date, name)
VALUES ('2025-01-01', '신정'),
       ('2025-01-27', '임시공휴일'),
       ('2025-01-28', '설날 연휴'),
       ('2025-01-29', '설날'),
       ('2025-01-30', '설날 연휴'),
       ('2025-03-01', '삼일절'),
       ('2025-03-03', '대체공휴일(삼일절)'),
       ('2025-05-05', '어린이날·부처님오신날'),
       ('2025-05-06', '대체공휴일(어린이날·부처님오신날)'),
       ('2025-06-03', '제21대 대통령 선거'),
       ('2025-06-06', '현충일'),
       ('2025-08-15', '광복절'),
       ('2025-10-03', '개천절'),
       ('2025-10-05', '추석 연휴'),
       ('2025-10-06', '추석'),
       ('2025-10-07', '추석 연휴'),
       ('2025-10-08', '대체공휴일(추석)'),
       ('2025-10-09', '한글날'),
       ('2025-12-25', '성탄절'),
       ('2026-01-01', '신정'),
       ('2026-02-16', '설날 연휴'),
       ('2026-02-17', '설날'),
       ('2026-02-18', '설날 연휴'),
       ('2026-03-01', '삼일절'),
       ('2026-03-02', '대체공휴일(삼일절)'),
       ('2026-05-01', '노동절'),
       ('2026-05-05', '어린이날'),
       ('2026-05-24', '부처님오신날'),
       ('2026-05-25', '대체공휴일(부처님오신날)'),
       ('2026-06-03', '제9회 전국동시지방선거'),
       ('2026-06-06', '현충일'),
       ('2026-07-17', '제헌절'),
       ('2026-08-15', '광복절'),
       ('2026-08-17', '대체공휴일(광복절)'),
       ('2026-09-24', '추석 연휴'),
       ('2026-09-25', '추석'),
       ('2026-09-26', '추석 연휴'),
       ('2026-10-03', '개천절'),
       ('2026-10-05', '대체공휴일(개천절)'),
       ('2026-10-09', '한글날'),
       ('2026-12-25', '성탄절');

-- 객실 정가 (docs/요금표.xlsx). 성수기 판단과 다중 객실 예약 금액 분할의 가중치에 쓴다.
-- 리조트 프리미엄(139,000원 고정)은 호실이 정해지지 않아 일반 리조트 요금으로 둔다.
-- 일치하지 않는 금액은 판단에 쓰이지 않으므로 틀린 표가 들어가지는 않는다.
-- Villa II, III는 요금표에 없어 행이 없다 (성수기 판단에서 제외).
CREATE TABLE room_list_prices
(
    room_id         BIGINT         NOT NULL PRIMARY KEY,
    offpeak_weekday DECIMAL(15, 2) NOT NULL,
    offpeak_weekend DECIMAL(15, 2) NOT NULL,
    peak_weekday    DECIMAL(15, 2) NOT NULL,
    peak_weekend    DECIMAL(15, 2) NOT NULL
);

INSERT INTO room_list_prices (room_id, offpeak_weekday, offpeak_weekend, peak_weekday, peak_weekend)
SELECT r.id, p.offpeak_weekday, p.offpeak_weekend, p.peak_weekday, p.peak_weekend
FROM rooms r
         JOIN (SELECT '놀멍' AS name, 230000 AS offpeak_weekday, 250000 AS offpeak_weekend, 290000 AS peak_weekday, 290000 AS peak_weekend
               UNION ALL SELECT '물멍', 230000, 250000, 290000, 290000
               UNION ALL SELECT '쉬멍', 230000, 250000, 290000, 290000
               UNION ALL SELECT '불멍', 200000, 230000, 250000, 250000
               UNION ALL SELECT '수애기', 150000, 170000, 220000, 220000
               UNION ALL SELECT '곰새기', 150000, 170000, 220000, 220000
               UNION ALL SELECT '제돌이', 150000, 170000, 220000, 220000
               UNION ALL SELECT '태산이', 150000, 170000, 220000, 220000
               UNION ALL SELECT '춘삼이', 150000, 170000, 220000, 220000
               UNION ALL SELECT '바다', 100000, 120000, 170000, 170000
               UNION ALL SELECT '노을', 100000, 120000, 170000, 170000
               UNION ALL SELECT '해안', 110000, 130000, 200000, 200000
               UNION ALL SELECT '바람', 110000, 130000, 180000, 180000
               UNION ALL SELECT '고래', 130000, 150000, 200000, 200000
               UNION ALL SELECT 'PREMIUM_VILLA', 390000, 430000, 480000, 520000
               UNION ALL SELECT 'VILLA_1', 390000, 430000, 480000, 520000) p ON p.name = r.name;

INSERT INTO room_list_prices (room_id, offpeak_weekday, offpeak_weekend, peak_weekday, peak_weekend)
SELECT id, 89000, 89000, 129000, 129000
FROM rooms
WHERE type = 'RESORT';

-- 업소·날짜별 성수기 여부.
-- INFERRED: 예약 금액으로 자동 판단 (표가 없거나 동률이면 peak = NULL)
-- MANUAL: 관리자가 고친 값. 자동 재계산이 덮어쓰지 않는다.
CREATE TABLE season_days
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id    BIGINT      NOT NULL,
    stay_date      DATE        NOT NULL,
    peak           BOOLEAN,
    source         VARCHAR(20) NOT NULL,
    peak_votes     INT         NOT NULL DEFAULT 0,
    offpeak_votes  INT         NOT NULL DEFAULT 0,
    updated_at     DATETIME    NOT NULL,
    CONSTRAINT uq_season_days_property_date UNIQUE (property_id, stay_date)
);

-- 예약 단위 피처. 숙박일을 모르면(취소수수료만 있는 예약) NULL.
-- weekend: 주말 밤이 하나라도 있으면 TRUE
-- peak_season: 성수기 밤이 하나라도 있으면 TRUE, 판단된 밤이 모두 비수기면 FALSE, 판단된 밤이 없으면 NULL
ALTER TABLE reservations
    ADD COLUMN weekend     BOOLEAN NULL AFTER nights,
    ADD COLUMN peak_season BOOLEAN NULL AFTER weekend;
