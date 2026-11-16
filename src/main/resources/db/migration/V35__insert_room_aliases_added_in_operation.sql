-- 운영 중 '객실 지정' 화면에서 추가한 플랫폼 객실명 (2026-11-16 검증 DB 기준, V16 시드 이후 14개).
-- 2025년 정산 파일의 옛 객실명: 에어스트림1~5 → 수애기·곰새기·제돌이·태산이·춘삼이 (이름이 바뀐 에어스트림), 놀멍·불멍 옛 표기.
-- 객실 id는 DB마다 다를 수 있어 객실 이름으로 찾는다. 이미 같은 별칭이 있는 DB(이 화면으로 추가한 DB)에서는 건너뛴다.
-- 새로 쌓인 별칭을 다음 배포에 넣을 때도 같은 방식으로 새 마이그레이션을 만든다 (docs §6.4.5).
INSERT IGNORE INTO room_aliases (source, external_name, room_id)
SELECT 'DDNAYO', '에어스트림5', id FROM rooms WHERE name = '춘삼이'
UNION ALL
SELECT 'DDNAYO', '스페이스카라반 놀멍 (애견동반가능)', id FROM rooms WHERE name = '놀멍'
UNION ALL
SELECT 'DDNAYO', '에어스트림3 (애견동반가능)((오션뷰&돌고래뷰)', id FROM rooms WHERE name = '제돌이'
UNION ALL
SELECT 'DDNAYO', '에어스트림4 (애견동반가능)(오션뷰&돌고래뷰)', id FROM rooms WHERE name = '태산이'
UNION ALL
SELECT 'DDNAYO', '에어스트림1 (오션뷰&돌고래뷰)', id FROM rooms WHERE name = '수애기'
UNION ALL
SELECT 'DDNAYO', '에어스트림2 (오션뷰&돌고래뷰)', id FROM rooms WHERE name = '곰새기'
UNION ALL
SELECT 'DDNAYO', '에어스트림5 (오션뷰&돌고래뷰)', id FROM rooms WHERE name = '춘삼이'
UNION ALL
SELECT 'YEOGI', '스페이스카라반 불멍(오션뷰&돌고래뷰)', id FROM rooms WHERE name = '불멍'
UNION ALL
SELECT 'YEOGI', '춘삼이', id FROM rooms WHERE name = '춘삼이'
UNION ALL
SELECT 'YEOGI', '에어스트림2 (오션뷰&돌고래뷰)', id FROM rooms WHERE name = '곰새기'
UNION ALL
SELECT 'YEOGI', '에어스트림1 (오션뷰&돌고래뷰)', id FROM rooms WHERE name = '수애기'
UNION ALL
SELECT 'YEOGI', '에어스트림5 (오션뷰&돌고래뷰)', id FROM rooms WHERE name = '춘삼이'
UNION ALL
SELECT 'YEOGI', '에어스트림3 (애견동반가능)((오션뷰&돌고래뷰)', id FROM rooms WHERE name = '제돌이'
UNION ALL
SELECT 'YEOGI', '에어스트림4 (애견동반가능)(오션뷰&돌고래뷰)', id FROM rooms WHERE name = '태산이';
