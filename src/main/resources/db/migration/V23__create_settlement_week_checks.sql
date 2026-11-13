-- 정산 파일 현황: 파일이 없는 주를 관리자가 "확인 완료"로 표시한다.
-- 그 주에 판매가 없어 플랫폼이 정산 파일을 만들지 않은 경우 등.
-- 정산 계정은 파일명에서 읽은 이름 (SettlementFileName.account) 그대로.
CREATE TABLE settlement_week_checks
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    source     VARCHAR(20)  NOT NULL,
    account    VARCHAR(255) NOT NULL,
    -- 그 주의 월요일
    week_start DATE         NOT NULL,
    note       VARCHAR(255),
    created_at DATETIME     NOT NULL,
    CONSTRAINT uq_settlement_week_checks UNIQUE (source, account, week_start)
);
