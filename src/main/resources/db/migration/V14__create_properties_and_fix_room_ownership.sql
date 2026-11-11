-- 업소(운영 단위)와 사업자(명의 단위)를 분리하고, 객실 명의를 실제와 맞춘다.

CREATE TABLE properties
(
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50)  NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uq_properties_code UNIQUE (code)
);

INSERT INTO properties (code, name)
VALUES ('SUNSETCOAST_CAMPING', '선셋코스트 카라반&캠핑장'),
       ('STAYDND_POOLVILLA', '스테이디앤디풀빌라');

-- 외부 소유주는 사업자번호/주소를 모를 수 있다.
ALTER TABLE business_owners
    ADD COLUMN ownership_type VARCHAR(20) NOT NULL DEFAULT 'FAMILY' AFTER name,
    MODIFY business_number VARCHAR(20) NULL,
    MODIFY address TEXT NULL;

INSERT INTO business_owners (name, ownership_type, status, created_at, updated_at)
VALUES ('어라운드대정', 'EXTERNAL', 'ACTIVE', now(), now()),
       ('대정시크릿', 'EXTERNAL', 'ACTIVE', now(), now());

-- 리조트 명의 정정: 대흥씨엔에스 명의는 203, 305호 (201호는 선셋코스트)
UPDATE rooms
SET business_owner_id = (SELECT id FROM business_owners WHERE name = '선셋코스트')
WHERE name = 'RESORT_201';

UPDATE rooms
SET business_owner_id = (SELECT id FROM business_owners WHERE name = '대흥씨엔에스(주)')
WHERE name = 'RESORT_203';

-- 외부 소유 풀빌라
INSERT INTO rooms (name, type, status, business_owner_id)
VALUES ('VILLA_2', 'POOL_VILLA', 'ACTIVE', (SELECT id FROM business_owners WHERE name = '어라운드대정')),
       ('VILLA_3', 'POOL_VILLA', 'ACTIVE', (SELECT id FROM business_owners WHERE name = '대정시크릿'));

ALTER TABLE rooms
    ADD COLUMN property_id BIGINT NULL AFTER status;

UPDATE rooms
SET property_id = (SELECT id FROM properties WHERE code = 'SUNSETCOAST_CAMPING')
WHERE type IN ('SPACE_CARAVAN', 'AIR_STREAM', 'CARAVAN');

UPDATE rooms
SET property_id = (SELECT id FROM properties WHERE code = 'STAYDND_POOLVILLA')
WHERE type IN ('RESORT', 'POOL_VILLA');

ALTER TABLE rooms
    MODIFY property_id BIGINT NOT NULL,
    ADD INDEX idx_rooms_property (property_id),
    ADD INDEX idx_rooms_business_owner (business_owner_id);
