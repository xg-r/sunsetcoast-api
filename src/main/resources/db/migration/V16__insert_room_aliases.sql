-- 2026년 8월 정산 파일, 10월 예약 파일에서 확인한 플랫폼 객실명.
-- 여러 객실이 묶인 행(쉼표 포함)은 매핑하지 않는다.
INSERT INTO room_aliases (source, external_name, room_id)
SELECT 'DDNAYO', 'Bada 바다 (오션뷰&돌고래뷰)', id FROM rooms WHERE name = '바다'
UNION ALL
SELECT 'DDNAYO', 'Baram 바람 (애견동반가능)(오션뷰&돌고래', id FROM rooms WHERE name = '바람'
UNION ALL
SELECT 'DDNAYO', 'Baram 바람 (애견동반가능)(오션뷰&돌고래뷰)', id FROM rooms WHERE name = '바람'
UNION ALL
SELECT 'DDNAYO', 'Gorae 고래 (애견동반가능)(오션뷰&돌고래뷰)', id FROM rooms WHERE name = '고래'
UNION ALL
SELECT 'DDNAYO', 'Haean 해안 (애견동반가능)(오션뷰&돌고래뷰)', id FROM rooms WHERE name = '해안'
UNION ALL
SELECT 'DDNAYO', 'Noeul 노을 (오션뷰&돌고래뷰)', id FROM rooms WHERE name = '노을'
UNION ALL
SELECT 'DDNAYO', 'Resort B - 305호(오션뷰&돌고래뷰)', id FROM rooms WHERE name = 'RESORT_305'
UNION ALL
SELECT 'DDNAYO', '곰새기 (오션뷰&돌고래뷰)', id FROM rooms WHERE name = '곰새기'
UNION ALL
SELECT 'DDNAYO', '수애기 (오션뷰&돌고래뷰)', id FROM rooms WHERE name = '수애기'
UNION ALL
SELECT 'DDNAYO', '스페이스카라반 놀멍 (애견동반)(오션뷰&돌고래뷰)', id FROM rooms WHERE name = '놀멍'
UNION ALL
SELECT 'DDNAYO', '스페이스카라반 물멍(오션뷰&돌고래뷰)', id FROM rooms WHERE name = '물멍'
UNION ALL
SELECT 'DDNAYO', '스페이스카라반 불멍(오션뷰&돌고래뷰)', id FROM rooms WHERE name = '불멍'
UNION ALL
SELECT 'DDNAYO', '스페이스카라반 쉬멍(오션뷰&돌고래뷰)', id FROM rooms WHERE name = '쉬멍'
UNION ALL
SELECT 'DDNAYO', '제돌이 (애견동반가능)((오션뷰&돌고래뷰)', id FROM rooms WHERE name = '제돌이'
UNION ALL
SELECT 'DDNAYO', '춘삼이 (오션뷰&돌고래뷰)', id FROM rooms WHERE name = '춘삼이'
UNION ALL
SELECT 'DDNAYO', '태산이(애견동반가능)(오션뷰&돌고래뷰)', id FROM rooms WHERE name = '태산이'
UNION ALL
SELECT 'NOL', '1-PREMIUM-독채(VIP룸 리조트 전 객실 오션뷰 & 돌고래 뷰)', id FROM rooms WHERE name = 'PREMIUM_VILLA'
UNION ALL
SELECT 'NOL', '2-Villa I-독채(리조트 전 객실 오션뷰 & 돌고래 뷰)', id FROM rooms WHERE name = 'VILLA_1'
UNION ALL
SELECT 'NOL', '3-Villa II-독채', id FROM rooms WHERE name = 'VILLA_2'
UNION ALL
SELECT 'NOL', '4-Villa III-독채', id FROM rooms WHERE name = 'VILLA_3'
UNION ALL
SELECT 'NOL', 'Resort A 201호(리조트 전 객실 오션뷰 & 돌고래 뷰)', id FROM rooms WHERE name = 'RESORT_201'
UNION ALL
SELECT 'NOL', 'Resort A 202호(리조트 전 객실 오션뷰 & 돌고래 뷰)', id FROM rooms WHERE name = 'RESORT_202'
UNION ALL
SELECT 'NOL', 'Resort A 203호(리조트 전 객실 오션뷰 & 돌고래 뷰)', id FROM rooms WHERE name = 'RESORT_203'
UNION ALL
SELECT 'NOL', 'Resort A 204호(리조트 전 객실 오션뷰 & 돌고래 뷰)', id FROM rooms WHERE name = 'RESORT_204'
UNION ALL
SELECT 'NOL', 'Resort A 205호(Premium 리조트 전 객실 오션뷰 & 돌고래 뷰)', id FROM rooms WHERE name = 'RESORT_205'
UNION ALL
SELECT 'NOL', 'Resort A 206호(리조트 전 객실 오션뷰 & 돌고래 뷰)', id FROM rooms WHERE name = 'RESORT_206'
UNION ALL
SELECT 'NOL', 'Resort A 207호(리조트 전 객실 오션뷰 & 돌고래 뷰)', id FROM rooms WHERE name = 'RESORT_207'
UNION ALL
SELECT 'NOL', 'Resort B 301호(리조트 전 객실 오션뷰 & 돌고래 뷰)', id FROM rooms WHERE name = 'RESORT_301'
UNION ALL
SELECT 'NOL', 'Resort B 302호(리조트 전 객실 오션뷰 & 돌고래 뷰)', id FROM rooms WHERE name = 'RESORT_302'
UNION ALL
SELECT 'NOL', 'Resort B 303호(리조트 전 객실 오션뷰 & 돌고래 뷰)', id FROM rooms WHERE name = 'RESORT_303'
UNION ALL
SELECT 'NOL', 'Resort B 304호(리조트 전 객실 오션뷰 & 돌고래 뷰)', id FROM rooms WHERE name = 'RESORT_304'
UNION ALL
SELECT 'NOL', 'Resort B 305호(리조트 전 객실 오션뷰 & 돌고래 뷰)', id FROM rooms WHERE name = 'RESORT_305'
UNION ALL
SELECT 'NOL', 'Resort B 306호(리조트 전 객실 오션뷰 & 돌고래 뷰)', id FROM rooms WHERE name = 'RESORT_306'
UNION ALL
SELECT 'NOL', 'Resort B 307호', id FROM rooms WHERE name = 'RESORT_307'
UNION ALL
SELECT 'YEOGI', 'Bada 바다 (오션뷰&돌고래뷰)', id FROM rooms WHERE name = '바다'
UNION ALL
SELECT 'YEOGI', 'Baram 바람 (애견동반가능)(오션뷰&돌고래뷰)', id FROM rooms WHERE name = '바람'
UNION ALL
SELECT 'YEOGI', 'Gorae 고래 (애견동반가능)(오션뷰&돌고래뷰)', id FROM rooms WHERE name = '고래'
UNION ALL
SELECT 'YEOGI', 'Haean 해안 (애견동반가능)(오션뷰&돌고래뷰)', id FROM rooms WHERE name = '해안'
UNION ALL
SELECT 'YEOGI', 'Noeul 노을 (오션뷰&돌고래뷰)', id FROM rooms WHERE name = '노을'
UNION ALL
SELECT 'YEOGI', 'PREMIUM독채(오션뷰&돌고래뷰)', id FROM rooms WHERE name = 'PREMIUM_VILLA'
UNION ALL
SELECT 'YEOGI', 'Resort A - 201호(오션뷰&돌고래뷰)', id FROM rooms WHERE name = 'RESORT_201'
UNION ALL
SELECT 'YEOGI', 'Resort A - 202호(오션뷰&돌고래뷰)', id FROM rooms WHERE name = 'RESORT_202'
UNION ALL
SELECT 'YEOGI', 'Resort A - 203호(오션뷰&돌고래뷰)', id FROM rooms WHERE name = 'RESORT_203'
UNION ALL
SELECT 'YEOGI', 'Resort A - 204호(오션뷰&돌고래뷰)', id FROM rooms WHERE name = 'RESORT_204'
UNION ALL
SELECT 'YEOGI', 'Resort A - 205(Premium)(오션뷰&돌고래뷰)', id FROM rooms WHERE name = 'RESORT_205'
UNION ALL
SELECT 'YEOGI', 'Resort A - 206호(오션뷰&돌고래뷰)', id FROM rooms WHERE name = 'RESORT_206'
UNION ALL
SELECT 'YEOGI', 'Resort A - 207호(오션뷰&돌고래뷰)', id FROM rooms WHERE name = 'RESORT_207'
UNION ALL
SELECT 'YEOGI', 'Resort B - 301호(오션뷰&돌고래뷰)', id FROM rooms WHERE name = 'RESORT_301'
UNION ALL
SELECT 'YEOGI', 'Resort B - 302호(오션뷰&돌고래뷰)', id FROM rooms WHERE name = 'RESORT_302'
UNION ALL
SELECT 'YEOGI', 'Resort B - 303호(오션뷰&돌고래뷰)', id FROM rooms WHERE name = 'RESORT_303'
UNION ALL
SELECT 'YEOGI', 'Resort B - 304호(오션뷰&돌고래뷰)', id FROM rooms WHERE name = 'RESORT_304'
UNION ALL
SELECT 'YEOGI', 'Resort B - 305호(오션뷰&돌고래뷰)', id FROM rooms WHERE name = 'RESORT_305'
UNION ALL
SELECT 'YEOGI', 'Resort B - 306호(오션뷰&돌고래뷰)', id FROM rooms WHERE name = 'RESORT_306'
UNION ALL
SELECT 'YEOGI', 'Resort B - 307호(오션뷰&돌고래뷰)', id FROM rooms WHERE name = 'RESORT_307'
UNION ALL
SELECT 'YEOGI', 'Villa I(오션뷰&돌고래뷰)', id FROM rooms WHERE name = 'VILLA_1'
UNION ALL
SELECT 'YEOGI', 'Villa II', id FROM rooms WHERE name = 'VILLA_2'
UNION ALL
SELECT 'YEOGI', 'Villa lll', id FROM rooms WHERE name = 'VILLA_3'
UNION ALL
SELECT 'YEOGI', '곰새기', id FROM rooms WHERE name = '곰새기'
UNION ALL
SELECT 'YEOGI', '수애기', id FROM rooms WHERE name = '수애기'
UNION ALL
SELECT 'YEOGI', '스페이스카라반 놀멍 (애견동반)(오션뷰&돌고래뷰)', id FROM rooms WHERE name = '놀멍'
UNION ALL
SELECT 'YEOGI', '스페이스카라반 물멍(오션뷰&돌고래뷰)', id FROM rooms WHERE name = '물멍'
UNION ALL
SELECT 'YEOGI', '스페이스카라반 쉬멍(오션뷰&돌고래뷰)', id FROM rooms WHERE name = '쉬멍'
UNION ALL
SELECT 'YEOGI', '제돌이', id FROM rooms WHERE name = '제돌이'
UNION ALL
SELECT 'YEOGI', '태산이', id FROM rooms WHERE name = '태산이';
