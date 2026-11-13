-- 역할을 users.role 컬럼 대신 roles + user_roles(사용자-역할 조인) 테이블로 관리한다.
-- 한 사용자가 역할을 여러 개 가질 수 있다. 처음 가입하면 USER 하나.
CREATE TABLE roles
(
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    -- 코드에서 쓰는 값 (UserRole). Spring Security 권한은 "ROLE_" + code
    code VARCHAR(20) NOT NULL,
    name VARCHAR(50) NOT NULL,
    CONSTRAINT uq_roles_code UNIQUE (code)
);

INSERT INTO roles (code, name)
VALUES ('SUPER_ADMIN', '슈퍼어드민'),
       ('SITE_MANAGER', '현장관리자'),
       ('USER', '일반사용자');

CREATE TABLE user_roles
(
    user_id    BIGINT   NOT NULL,
    role_id    BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

-- 기존 역할 옮기기
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
         JOIN roles r ON r.code = u.role;

ALTER TABLE users
    DROP COLUMN role;
