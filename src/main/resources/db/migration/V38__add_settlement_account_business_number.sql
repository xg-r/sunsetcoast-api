-- 떠나요 정산 계정의 사업자번호 (숫자 10자리, 하이픈 없이).
-- 떠나요에서 받은 그대로의 AdjustCalendar 파일은 파일명에 업소명이 없고 행마다 사업자번호만 있어서,
-- 이 번호로 파일을 등록한 정산 계정에 붙인다.
ALTER TABLE settlement_accounts
    ADD COLUMN business_number VARCHAR(10) NULL AFTER property_id;
