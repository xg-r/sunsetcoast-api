-- 관리비 입금 확인.
-- 정산 파일 업로드·재계산은 금액(calculated_amount)만 정하고, 실제로 입금했는지는 관리자가 확인해 표시한다.
-- paid_amount: 입금 완료로 표시할 때의 지급액. 그 뒤 업로드·재계산으로 금액이 바뀌면 화면에서 차액을 알려 준다.
-- paid_by: 표시한 사용자 (users.id)
ALTER TABLE management_fees
    ADD COLUMN paid_at     DATETIME       NULL AFTER overridden_at,
    ADD COLUMN paid_amount DECIMAL(15, 2) NULL AFTER paid_at,
    ADD COLUMN paid_by     BIGINT         NULL AFTER paid_amount;
