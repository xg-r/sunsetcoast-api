-- 로그인 사용자 (네이버·카카오 OAuth)
--
-- 역할: SUPER_ADMIN(본사 직원), SITE_MANAGER(현장 관리자), USER(일반 사용자).
-- 처음 로그인하면 USER. 관리자 화면은 SUPER_ADMIN·SITE_MANAGER만 쓸 수 있고,
-- 역할은 SUPER_ADMIN이 사용자 관리 화면에서 바꾼다.
-- 첫 SUPER_ADMIN은 설정(AUTH_SUPER_ADMINS="KAKAO:123,NAVER:abc")으로 지정한다.
-- provider DEV는 로컬 개발용 로그인(sunsetcoast.auth.dev-login-enabled)으로만 생긴다.
CREATE TABLE users
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider         VARCHAR(20)  NOT NULL,
    -- 플랫폼이 주는 사용자 식별자 (카카오 회원번호, 네이버 고유 ID)
    provider_user_id VARCHAR(100) NOT NULL,
    nickname         VARCHAR(100),
    -- 동의한 경우에만 받는다
    email            VARCHAR(255),
    role             VARCHAR(20)  NOT NULL,
    created_at       DATETIME     NOT NULL,
    last_login_at    DATETIME,
    CONSTRAINT uq_users_provider UNIQUE (provider, provider_user_id)
);
