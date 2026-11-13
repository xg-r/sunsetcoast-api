-- OAuth 로그인으로 받는 회원 정보 (사용자가 동의한 경우에만 받는다)
ALTER TABLE users
    -- 실명 (닉네임과 별개)
    ADD COLUMN name     VARCHAR(100) AFTER nickname,
    -- MALE, FEMALE
    ADD COLUMN gender   VARCHAR(10)  AFTER email,
    -- 생일 "MM-DD" (출생연도는 받지 않는다)
    ADD COLUMN birthday VARCHAR(5)   AFTER gender,
    -- 휴대전화 "010-1234-5678"
    ADD COLUMN mobile   VARCHAR(20)  AFTER birthday;
