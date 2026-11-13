-- 기존 settlements 테이블은 저장 로직이 없어 비어 있다. 예약/정산 행 구조로 대체한다.
DROP TABLE IF EXISTS settlements;

-- 플랫폼 객실명 -> 내부 객실
CREATE TABLE room_aliases
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    source        VARCHAR(20)  NOT NULL,
    external_name VARCHAR(255) NOT NULL,
    room_id       BIGINT       NOT NULL,
    CONSTRAINT uq_room_aliases_source_name UNIQUE (source, external_name)
);

-- 업로드 1건 = 파일 1개
CREATE TABLE import_batches
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    source         VARCHAR(20)  NOT NULL,
    file_kind      VARCHAR(20)  NOT NULL,
    file_name      VARCHAR(255) NOT NULL,
    file_hash      CHAR(64)     NOT NULL,
    total_rows     INT          NOT NULL DEFAULT 0,
    inserted_rows  INT          NOT NULL DEFAULT 0,
    duplicate_rows INT          NOT NULL DEFAULT 0,
    changed_rows   INT          NOT NULL DEFAULT 0,
    skipped_rows   INT          NOT NULL DEFAULT 0,
    failed_rows    INT          NOT NULL DEFAULT 0,
    unmapped_rows  INT          NOT NULL DEFAULT 0,
    created_at     DATETIME     NOT NULL,
    CONSTRAINT uq_import_batches_file_hash UNIQUE (file_hash)
);

-- 원본 행 보존 (연락처 등 개인정보 컬럼은 제외하고 저장)
CREATE TABLE import_rows
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    import_batch_id BIGINT       NOT NULL,
    row_no          INT          NOT NULL,
    result          VARCHAR(20)  NOT NULL,
    message         VARCHAR(500),
    payload         JSON         NOT NULL,
    INDEX idx_import_rows_batch (import_batch_id)
);

CREATE TABLE reservations
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    source             VARCHAR(20)  NOT NULL,
    reservation_number VARCHAR(50)  NOT NULL,
    room_id            BIGINT       NOT NULL,
    sales_channel      VARCHAR(20)  NOT NULL,
    sales_channel_raw  VARCHAR(100),
    booker_name        VARCHAR(100),
    reserved_at        DATETIME,
    check_in_date      DATE,
    nights             INT,
    status             VARCHAR(20)  NOT NULL,
    payment_receiver   VARCHAR(20)  NOT NULL DEFAULT 'US',
    amount_status      VARCHAR(20)  NOT NULL,
    created_at         DATETIME     NOT NULL,
    updated_at         DATETIME     NOT NULL,
    CONSTRAINT uq_reservations_source_number UNIQUE (source, reservation_number),
    INDEX idx_reservations_room_check_in (room_id, check_in_date)
);

-- 정산 행: 1박(ROOM_NIGHT), 취소수수료, 입실 후 취소(차감) 등
-- seq: 같은 예약/유형/날짜 행이 한 파일에 여러 개일 때의 순번 (예: NOL 취소수수료 2건)
CREATE TABLE settlement_lines
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id    BIGINT         NOT NULL,
    import_batch_id   BIGINT         NOT NULL,
    line_type         VARCHAR(30)    NOT NULL,
    stay_date         DATE           NOT NULL,
    seq               INT            NOT NULL DEFAULT 0,
    settled_on        DATE,
    gross_amount      DECIMAL(15, 2) NOT NULL,
    partner_discount  DECIMAL(15, 2) NOT NULL DEFAULT 0,
    platform_discount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    commission_rate   DECIMAL(5, 2),
    commission_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    other_fee         DECIMAL(15, 2) NOT NULL DEFAULT 0,
    net_amount        DECIMAL(15, 2) NOT NULL,
    split_estimated   BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at        DATETIME       NOT NULL,
    updated_at        DATETIME       NOT NULL,
    CONSTRAINT uq_settlement_lines_key UNIQUE (reservation_id, line_type, stay_date, seq),
    INDEX idx_settlement_lines_stay_date (stay_date),
    INDEX idx_settlement_lines_batch (import_batch_id)
);

-- 금액이 바뀐 행의 이전 값
CREATE TABLE settlement_line_revisions
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    settlement_line_id BIGINT   NOT NULL,
    import_batch_id    BIGINT   NOT NULL,
    previous_values    JSON     NOT NULL,
    created_at         DATETIME NOT NULL,
    INDEX idx_settlement_line_revisions_line (settlement_line_id)
);
