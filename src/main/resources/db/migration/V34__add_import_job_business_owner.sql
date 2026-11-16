-- 계좌 거래내역 업로드에서 고른 사업자. 거래내역 파일에 계좌번호 줄이 없을 수 있어 업로드할 때 사업자를 고른다.
ALTER TABLE import_jobs
    ADD COLUMN business_owner_id BIGINT NULL AFTER kind;
