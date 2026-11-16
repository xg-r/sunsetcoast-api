-- 이메일 하나로는 한 계정만 가입한다 (네이버·카카오에 같은 이메일로 두 번 가입하지 않도록).
-- 이메일을 보내지 않은 계정(NULL)은 여러 개여도 된다. 열 정렬 규칙(utf8mb4_0900_ai_ci)이라 대소문자는 같은 이메일로 본다.
-- 적용 전에 중복을 정리해야 한다:
--   SELECT email, COUNT(*) FROM users WHERE email IS NOT NULL GROUP BY email HAVING COUNT(*) > 1;
-- (2026-11-16 로컬 DB에서는 같은 이메일로 두 번 가입한 네이버 계정을 지운 뒤 적용)
ALTER TABLE users
    ADD CONSTRAINT uq_users_email UNIQUE (email);
