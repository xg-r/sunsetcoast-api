-- 파일 업로드 비동기 처리.
-- 업로드 요청은 파일마다 작업(import_jobs)만 만들고 바로 응답한다. 작업은 백그라운드에서 한 번에 하나씩 처리한다
-- (같은 예약을 동시에 쓰지 않도록). 파일 내용은 처리가 끝날 때까지 file_bytes에 두어, 서버가 재시작돼도 다시 처리할 수 있다.
--
-- kind: SETTLEMENT(정산 파일), BANK(계좌 거래내역)
-- status: QUEUED → RUNNING → SUCCEEDED / FAILED
-- result_json: 처리 결과 (정산은 SettlementImportResult, 계좌는 BankJobResult)
CREATE TABLE import_jobs
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    kind          VARCHAR(20)  NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    file_hash     CHAR(64)     NOT NULL,
    file_bytes    MEDIUMBLOB,
    status        VARCHAR(20)  NOT NULL,
    result_json   JSON,
    error_message VARCHAR(1000),
    requested_by  BIGINT,
    created_at    DATETIME     NOT NULL,
    started_at    DATETIME,
    finished_at   DATETIME,
    INDEX idx_import_jobs_status (status),
    INDEX idx_import_jobs_hash (file_hash)
);

-- 사용자 알림 (지금은 업로드 실패). 화면 사이드바에 읽지 않은 수가 보인다.
-- level: ERROR, WARN, INFO
CREATE TABLE notifications
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT        NOT NULL,
    level      VARCHAR(10)   NOT NULL,
    title      VARCHAR(255)  NOT NULL,
    message    VARCHAR(1000),
    link       VARCHAR(255),
    read_at    DATETIME,
    created_at DATETIME      NOT NULL,
    INDEX idx_notifications_user (user_id, read_at),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
