-- 정산 파일 현황에 나와야 하는 정산 계정.
-- 파일을 한 번도 올리지 않은 계정도 빠진 주로 보이게 하려고 등록해 둔다.
-- account는 파일명에서 읽는 이름 (SettlementFileName.account)과 같아야 한다.
--   떠나요·여기어때: 정산내역_{account}_yyyy-MM-dd[_떠나요].xls(x)
--   NOL: 정산상세내역_{업소}_{account}_MMdd.xls
CREATE TABLE settlement_accounts
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    source      VARCHAR(20)  NOT NULL,
    account     VARCHAR(255) NOT NULL,
    property_id BIGINT,
    -- 이 주(월요일)부터 정산 파일이 있어야 한다
    started_on  DATE         NOT NULL,
    created_at  DATETIME     NOT NULL,
    CONSTRAINT uq_settlement_accounts UNIQUE (source, account),
    CONSTRAINT fk_settlement_accounts_property FOREIGN KEY (property_id) REFERENCES properties (id)
);

-- 2026-08 정산 파일부터 관리한다
INSERT INTO settlement_accounts (source, account, property_id, started_on, created_at)
SELECT v.source, v.account, p.id, '2026-08-03', NOW()
FROM (SELECT 'DDNAYO' AS source, '서귀포 선셋코스트 카라반&캠핑장' AS account, 'SUNSETCOAST_CAMPING' AS property_code
      UNION ALL SELECT 'YEOGI', '서귀포 선셋코스트 카라반&캠핑장', 'SUNSETCOAST_CAMPING'
      UNION ALL SELECT 'DDNAYO', '서귀포 스테이 디앤디 풀빌라', 'STAYDND_POOLVILLA'
      UNION ALL SELECT 'YEOGI', '서귀포 스테이 디앤디 풀빌라', 'STAYDND_POOLVILLA'
      UNION ALL SELECT 'NOL', '대정시크릿', 'STAYDND_POOLVILLA'
      UNION ALL SELECT 'NOL', '대흥cns', 'STAYDND_POOLVILLA'
      UNION ALL SELECT 'NOL', '선셋코스트', 'STAYDND_POOLVILLA'
      UNION ALL SELECT 'NOL', '스테이디앤디', 'STAYDND_POOLVILLA'
      UNION ALL SELECT 'NOL', '어라운드대정', 'STAYDND_POOLVILLA') v
         LEFT JOIN properties p ON p.code = v.property_code;
