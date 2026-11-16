-- 관리비 확인: 관리자가 실제 자료와 한 건씩 맞춰 보고 '맞다'고 표시한다 (입금 완료와 별개).
-- verified_amount: 확인할 때의 지급액. 그 뒤 업로드·재계산·수정으로 금액이 바뀌면 화면이 '다시 확인'으로 알린다.
ALTER TABLE management_fees
    ADD COLUMN verified_at     DATETIME       NULL AFTER paid_by,
    ADD COLUMN verified_amount DECIMAL(15, 2) NULL AFTER verified_at,
    ADD COLUMN verified_by     BIGINT         NULL AFTER verified_amount;
