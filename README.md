# sunsetcoast-api

제주 숙박 사업장(선셋코스트 카라반&캠핑장, 스테이디앤디풀빌라)의 **운영 데이터 플랫폼 백엔드**입니다.
OTA 정산 파일(떠나요·여기어때·NOL)과 계좌 거래내역을 올리면 예약·매출·현장 관리비·현금흐름을 한곳에서 관리하고 분석합니다.

- 관리자 화면: [sunsetcoast-admin-web](https://github.com/xg-r/sunsetcoast-admin-web) (Next.js)
- 설계·진행 현황·인수인계: [`docs/운영데이터플랫폼.md`](docs/운영데이터플랫폼.md) (작업을 이어갈 때 §6부터 읽기)

## 주요 기능

| 영역 | 내용 |
|---|---|
| 정산 파일 업로드 | 떠나요·여기어때·NOL 파일을 내용으로 판별해 예약·1박 정산 행으로 저장. 같은 파일·행은 중복으로 거름. 백그라운드 처리와 실패 알림, 잘못 올린 파일 삭제(이전 값 복원) |
| 파일 현황 | 정산 계정 × 주마다 파일이 올라왔는지 확인 (떠나요 원본 파일은 사업자번호·체크아웃 주로 추정) |
| 예약 | OTA 예약 + 직접 입력(전화·현장) 예약, 다중 객실, 겹침 확인, 성수기·주말 판단 |
| 현장 관리비 | 예약 객실마다 단가로 자동 계산, 관리자 수정, 확인 표시, 입금 완료 표시, 사업자 × 월 입금 내역 |
| 계좌 분석 | 은행 거래내역 엑셀 업로드, 자동 분류(규칙 저장), 월별 현금흐름·비용 구조, OTA 입금 ↔ 정산 파일 대조, 관계사 정산 |
| 매출 분석 | 예약 기준 월 매출 전년 동월 비교, 판매 채널, 예약 시점 |
| 인증·보안 | 네이버·카카오 로그인, 역할(슈퍼어드민·현장관리자·일반), 로그인 기록(조회만, 1년 보관) |

## 기술 스택

- Kotlin 2.2, Java 21 (Amazon Corretto), Spring Boot 4.0 (Web MVC, Data JPA, Security, OAuth2 Client)
- MySQL 8.4, Flyway
- Apache POI (xls/xlsx), springdoc-openapi (Swagger, 로컬만)
- 테스트: Kotest, JUnit 5

## 시작하기 (로컬)

### 1. 준비물

- JDK 21
- MySQL 8.4 (Docker 권장)

```bash
docker run -d --name sunsetcoast-mysql \
  -e MYSQL_ROOT_PASSWORD=<비밀번호> -e MYSQL_DATABASE=sunsetcoast \
  -p 3306:3306 mysql:8.4
```

스키마와 기본 데이터(사업자·업소·객실·객실 별칭·관리비 단가·공휴일 등)는 앱이 뜰 때 Flyway가 만듭니다.

### 2. 비밀값

프로젝트 루트에 `local-secrets.yml`을 만듭니다 (gitignore, 없어도 실행되지만 네이버·카카오 로그인은 안 됨).

```yaml
NAVER_CLIENT_ID: <네이버 앱 Client ID>
NAVER_CLIENT_SECRET: <네이버 앱 Client Secret>
KAKAO_CLIENT_ID: <카카오 REST API 키>
KAKAO_CLIENT_SECRET: <카카오 Client Secret>
AUTH_SUPER_ADMINS: NAVER:someone@example.com   # 첫 슈퍼어드민 (아래 '환경 변수' 참고)
```

### 3. 실행

```bash
# macOS / Linux
SPRING_PROFILES_ACTIVE=local DB_PASSWORD=<비밀번호> ./gradlew bootRun

# Windows (cmd)
set SPRING_PROFILES_ACTIVE=local&& set DB_PASSWORD=<비밀번호>&& gradlew.bat bootRun
```

- 다른 DB를 쓰려면 `SPRING_DATASOURCE_URL`을 함께 넘깁니다 (예: `jdbc:mysql://localhost:3307/sunsetcoast`).
- API: http://localhost:8080 , Swagger: http://localhost:8080/swagger-ui.html (local 프로필에서만)
- 관리자 화면은 [sunsetcoast-admin-web](https://github.com/xg-r/sunsetcoast-admin-web)을 띄우면 `/api/*`를 이 서버로 넘겨 줍니다.

> WSL에서 작업할 때: WSL에 JDK가 없으면 Windows Gradle(`gradlew.bat`)로 빌드·실행합니다. WSL에서는 Windows 앱의 `localhost:8080`에 바로 닿지 않을 수 있어 `curl.exe`로 호출합니다.

### 4. 개발용 로그인

local 프로필에서는 OAuth 키 없이 역할별로 로그인할 수 있습니다.

```bash
curl -X POST -H "X-API-Version: 1.0" "http://localhost:8080/auth/dev-login?role=SUPER_ADMIN"   # SITE_MANAGER, USER
```

관리자 화면 로그인 페이지에도 개발 모드에서만 버튼이 보입니다. 운영에서는 꺼져 있습니다 (`sunsetcoast.auth.dev-login-enabled`).

## 환경 변수 (운영)

| 이름 | 설명 |
|---|---|
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | MySQL 접속 |
| `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET` | 네이버 로그인 |
| `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET` | 카카오 로그인 (콘솔에서 Client Secret 사용 설정) |
| `AUTH_SUPER_ADMINS` | 로그인하면 슈퍼어드민이 되는 계정. `플랫폼:이메일` 또는 `플랫폼:계정 번호`를 쉼표로 (예: `NAVER:a@example.com,KAKAO:1234567`). 이메일은 그 플랫폼이 보내 준 이메일과만 비교 |

- 네이버·카카오 개발자센터 Redirect URI: `{관리자 화면 주소}/login/oauth2/code/naver`, `…/kakao`
- 선택: `SUNSETCOAST_LOGINEVENTS_PURGECRON` (로그인 기록 정리 시각, 기본 매일 03:30)

## API 규칙

- **버전 헤더**: 모든 요청에 `X-API-Version: 1.0` (없으면 1.0으로 처리)
- **인증**: 세션 쿠키(`JSESSIONID`, 12시간, HttpOnly, SameSite=Lax). 로그인은 `/oauth2/authorization/{naver|kakao}`
- **CSRF**: 쿠키 `XSRF-TOKEN` 값을 `X-XSRF-TOKEN` 헤더로 보냄 (GET 제외)
- **권한**

  | 경로 | 역할 |
  |---|---|
  | `/auth/**` | 로그인한 누구나 |
  | `/management-fees/**`, `/admin/**`, `/reservations/direct/**`, `/bank/**`, `/analytics/**`, 정산 파일 삭제 | 슈퍼어드민 |
  | 그 밖의 관리자 API | 슈퍼어드민, 현장관리자 |

- **오류 응답**: `{"exception": "...", "message": "..."}` (메시지는 화면에 그대로 보여 줄 수 있는 한국어). 잘못된 값 400, 권한 없음 403, 없는 주소 404, 받지 않는 메서드 405
- 요청·응답 형식은 Swagger에서 확인합니다. 컨트롤러의 Swagger 설명은 `XxxControllerDocs` 인터페이스에 둡니다.

## DB 마이그레이션

`src/main/resources/db/migration/V{번호}__{설명}.sql` (Flyway). 이미 적용한 파일은 고치지 않고 새 번호로 추가합니다.

배포 전에 확인할 것:

- **V32 이메일 유니크**: 같은 이메일 계정이 둘 이상이면 실패합니다.
  `SELECT email, COUNT(*) FROM users WHERE email IS NOT NULL GROUP BY email HAVING COUNT(*) > 1;`
- **V37 로그인 기록 트리거**: DB 계정에 TRIGGER 권한이 필요합니다. 바이너리 로그가 켜진 MySQL이면 `log_bin_trust_function_creators=1`이 필요할 수 있습니다.
- **객실 별칭**: 운영 중 '객실 지정' 화면에서 추가한 별칭은 DB에만 쌓입니다. 새 DB에도 넣으려면 마이그레이션으로 옮깁니다 (V35 참고, 절차는 docs §6.4.5).

## 테스트

```bash
./gradlew test        # Windows: gradlew.bat test
```

- DB 없이 도는 단위 테스트입니다 (파서, 분류 규칙, 금액 계산, 관리비 규칙 등).
- 실제 정산·거래내역 파일로 검증하는 테스트(`SampleSettlementFilesTest`, `BankStatementTest`)는 고객 정보가 있는 원본 파일이 로컬(`docs/202610/정산`, `docs/거래내역샘플`)에 있을 때만 실행되고, 없으면 건너뜁니다.

## 보안·개인정보

- **백엔드를 인터넷에 직접 열지 않습니다.** 관리자 화면(또는 리버스 프록시)을 통해서만 받습니다. OAuth redirect URI를 화면 주소로 만들기 위해 `X-Forwarded-*` 헤더를 믿기 때문입니다 (`server.forward-headers-strategy: framework`).
- **플랫폼 정산 원본 파일·계좌 거래내역은 git에 올리지 않습니다** (고객 이름·연락처, 계좌 정보). `docs` 아래 원본 파일은 `.gitignore`로 제외됩니다. 코드·테스트에도 실제 계좌번호·이메일·실명을 넣지 않습니다.
- 로그인 기록은 조회만 됩니다. DB 트리거가 수정과 1년이 안 된 기록의 삭제를 막고, 1년 지난 기록은 매일 자동으로 지웁니다.

## 프로젝트 구조

```
src/main/kotlin/kr/sunsetcoast
├── common      공통 응답·예외 처리
├── config      보안(인증·인가), 웹(API 버전), 스케줄
├── controller  REST API (영역별 패키지, Swagger 설명은 *ControllerDocs)
└── domain      영역별 entity · repository · service · vo
    ├── settlement     정산 파일 파싱·저장·현황·삭제
    ├── importjob      업로드 백그라운드 작업
    ├── reservation    예약 (OTA·직접 입력)
    ├── managementfee  현장 관리비
    ├── staycalendar   성수기·주말 판단
    ├── bank           계좌 거래내역·분석
    ├── analytics      매출 분석
    ├── user, loginevent, notification
    └── property, room, businessowner, car, carreservation, sunsettime
```

## 문서

- [`docs/운영데이터플랫폼.md`](docs/운영데이터플랫폼.md): 요구사항, 데이터 모델, 플랫폼별 정산 파일 규칙, 진행 현황(§6)
- [`docs/차량렌트.md`](docs/차량렌트.md): 풀빌라 차량 무료 렌탈 요구사항
