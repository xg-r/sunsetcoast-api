-- 잘못 올린 정산 파일을 지울 때 그 파일이 바꾼 정산 행을 되돌리려면,
-- 바뀌기 전 행을 저장한 업로드와 정산일도 알아야 한다. (이전 금액은 previous_values에 있다)
-- 이 컬럼이 생기기 전의 이력은 NULL이다.
ALTER TABLE settlement_line_revisions
    ADD COLUMN previous_import_batch_id BIGINT NULL AFTER import_batch_id,
    ADD COLUMN previous_settled_on      DATE   NULL AFTER previous_values,
    ADD INDEX idx_settlement_line_revisions_batch (import_batch_id);
