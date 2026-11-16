-- 관리자 화면 로그인 기록 (접속기록). 로그인 성공·실패·로그아웃마다 한 줄.
--
-- 기록은 고치거나 지울 수 없다 (위·변조 방지):
--   - 앱에는 조회 API만 있고, 기록은 로그인·로그아웃 때 서버가 자동으로 남긴다
--   - 아래 트리거가 UPDATE는 모두, DELETE는 1년이 지나지 않은 기록을 거부한다 (DB에 직접 접속해도)
--   - 1년이 지난 기록은 매일 새벽 자동으로 지운다 (보관 기간 1년)
--
-- user_id: 로그인한 사용자 (실패해서 사용자가 없으면 NULL)
-- nickname·roles: 그때의 이름·역할 (나중에 바뀌어도 기록은 그대로)
-- event_type: LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT
CREATE TABLE login_events
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type     VARCHAR(20)  NOT NULL,
    user_id        BIGINT,
    provider       VARCHAR(20),
    nickname       VARCHAR(100),
    roles          VARCHAR(100),
    failure_reason VARCHAR(255),
    ip_address     VARCHAR(45),
    user_agent     VARCHAR(500),
    created_at     DATETIME     NOT NULL,
    INDEX idx_login_events_created (created_at),
    INDEX idx_login_events_user (user_id, created_at)
);

CREATE TRIGGER trg_login_events_no_update
    BEFORE UPDATE
    ON login_events
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '로그인 기록은 수정할 수 없습니다.';

CREATE TRIGGER trg_login_events_delete_after_retention
    BEFORE DELETE
    ON login_events
    FOR EACH ROW
BEGIN
    IF OLD.created_at >= NOW() - INTERVAL 1 YEAR THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '보관 기간(1년)이 지나지 않은 로그인 기록은 지울 수 없습니다.';
    END IF;
END;
