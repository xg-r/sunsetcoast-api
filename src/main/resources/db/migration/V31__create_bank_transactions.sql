-- 계좌 거래내역 (BANK-01)
--
-- 은행에서 받은 거래내역 엑셀(거래내역조회_입출식 예금)을 매달 올려 현금흐름·비용 구조를 본다.
-- 같은 기간을 겹쳐 올려도 되도록 거래는 (계좌, 거래일시, 출금, 입금, 거래후 잔액)으로 중복을 거른다.

-- 계좌. 파일의 '계좌번호:239-…-017 (선셋코스트)'에서 만든다. 괄호 안 이름이 사업자명과 같으면 사업자를 연결한다.
CREATE TABLE bank_accounts
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number    VARCHAR(50)  NOT NULL,
    holder_name       VARCHAR(100),
    business_owner_id BIGINT,
    created_at        DATETIME     NOT NULL,
    CONSTRAINT uq_bank_accounts_number UNIQUE (account_number)
);

-- 업로드 1건 = 파일 1개. 같은 파일(바이트 해시)은 다시 올릴 수 없다.
CREATE TABLE bank_statement_uploads
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    bank_account_id BIGINT       NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_hash       CHAR(64)     NOT NULL,
    period_from     DATETIME,
    period_to       DATETIME,
    total_rows      INT          NOT NULL DEFAULT 0,
    inserted_rows   INT          NOT NULL DEFAULT 0,
    duplicate_rows  INT          NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL,
    CONSTRAINT uq_bank_statement_uploads_hash UNIQUE (file_hash),
    CONSTRAINT fk_bank_statement_uploads_account FOREIGN KEY (bank_account_id) REFERENCES bank_accounts (id)
);

-- 거래 1건.
-- category: 자동 분류(category_source=AUTO) 또는 관리자가 고친 값(MANUAL). 자동 분류만 다시 계산한다.
CREATE TABLE bank_transactions
(
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    bank_account_id       BIGINT         NOT NULL,
    upload_id             BIGINT         NOT NULL,
    transacted_at         DATETIME       NOT NULL,
    withdrawal            DECIMAL(15, 2) NOT NULL DEFAULT 0,
    deposit               DECIMAL(15, 2) NOT NULL DEFAULT 0,
    balance               DECIMAL(15, 2) NOT NULL,
    description           VARCHAR(255)   NOT NULL,
    counterparty_account  VARCHAR(100),
    counterparty_bank     VARCHAR(50),
    counterparty_name     VARCHAR(100),
    memo                  VARCHAR(255),
    transaction_type      VARCHAR(50),
    category              VARCHAR(30)    NOT NULL,
    category_source       VARCHAR(10)    NOT NULL,
    note                  VARCHAR(255),
    created_at            DATETIME       NOT NULL,
    CONSTRAINT uq_bank_transactions_key UNIQUE (bank_account_id, transacted_at, withdrawal, deposit, balance),
    CONSTRAINT fk_bank_transactions_account FOREIGN KEY (bank_account_id) REFERENCES bank_accounts (id),
    CONSTRAINT fk_bank_transactions_upload FOREIGN KEY (upload_id) REFERENCES bank_statement_uploads (id),
    INDEX idx_bank_transactions_account_time (bank_account_id, transacted_at)
);

-- 관리자가 만든 분류 규칙. 거래내용(또는 상대 예금주명)에 keyword가 들어 있으면 category로 분류한다.
-- 코드에 있는 기본 규칙보다 먼저 적용한다. direction: IN(입금), OUT(출금), ANY
CREATE TABLE bank_category_rules
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword    VARCHAR(100) NOT NULL,
    direction  VARCHAR(10)  NOT NULL,
    category   VARCHAR(30)  NOT NULL,
    created_at DATETIME     NOT NULL,
    CONSTRAINT uq_bank_category_rules UNIQUE (keyword, direction)
);
