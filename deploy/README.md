# 운영 배포 (AWS EC2 + Docker Compose)

비용을 최소로: EC2 한 대에 Caddy(HTTPS) · 관리자 화면 · API · MySQL을 모두 올린다.

```
admin.sunsetcoast.kr ─┐                        ┌→ web (Next.js) ──(내부망 /api)──→ api
                      ├→ EC2 ─ Caddy (80/443) ─┤
api.sunsetcoast.kr  ──┘                        └→ api 의 /public/**, /me/** 만 ──→ api → mysql
sunsetcoast.kr → 지금처럼 S3 + CloudFront (이 서버와 무관)
```

- 밖으로 열리는 것은 Caddy(80/443)뿐이다. web·api·mysql은 Docker 내부망에만 있다.
- `api.sunsetcoast.kr`에서는 사용자용 경로(`/public/**`, `/me/**`)만 통과한다. 정산·관리비·계좌 같은 관리자 API는 404이고, 관리자 화면 서버를 거쳐서만 닿는다.
- 이미지는 GitHub Actions가 `main`에 올라올 때마다 ARM용으로 빌드해 GHCR(`ghcr.io/xg-r/sunsetcoast-api`, `ghcr.io/xg-r/sunsetcoast-admin-web`)에 올린다. SSM Run Command로 서버에서 `deploy.sh`를 실행해 반영한다 (SSH 포트를 열지 않는다, 7번).

## 비용 (서울 리전, 대략)

| 항목 | 온디맨드 | Savings Plan 1년 후 |
|---|---|---|
| EC2 t4g.small (ARM, 2 vCPU, 2GB) | 약 $15/월 | 약 $9~10/월 |
| 공인 IPv4 (탄력적 IP) | 약 $3.6/월 | 약 $3.6/월 |
| EBS gp3 12GB + 주간 스냅샷 | 약 $1.5/월 | 약 $1.5/월 |
| S3 백업 (3일마다, 30일 보관) | 몇십 원 | 몇십 원 |

정확한 금액은 [AWS 요금 계산기](https://calculator.aws/)로 확인한다. 로컬에서 같은 구성을 띄워 본 메모리 사용량: api 약 500MB, mysql 약 230MB, web 약 40MB, caddy 약 10MB.

## 1. AWS 준비

1. **EC2 인스턴스** (리전: 서울 `ap-northeast-2`)
   - AMI: Ubuntu Server 24.04 LTS (**arm64**)
   - 유형: `t4g.small`
   - 스토리지: gp3 12GB (기본값 8GB는 이미지 받다가 꽉 찬다)
   - 보안 그룹: 인바운드 TCP 80, 443, UDP 443 (0.0.0.0/0, ::/0). SSH 22는 사무실·집 IP만, 또는 열지 않고 Session Manager로 접속
2. **IAM 역할** (인스턴스에 연결)
   - `AmazonSSMManagedInstanceCore` (Session Manager 접속, GitHub Actions 배포)
   - 인라인 정책: 백업 버킷 경로에 쓰기만 + 설정값(Parameter Store) 읽기만
     ```json
     {
       "Version": "2012-10-17",
       "Statement": [
         { "Effect": "Allow", "Action": "s3:PutObject",
           "Resource": "arn:aws:s3:::<버킷>/sunsetcoast-db/*" },
         { "Effect": "Allow", "Action": "ssm:GetParametersByPath",
           "Resource": "arn:aws:ssm:ap-northeast-2:<계정ID>:parameter/sunsetcoast/prod" }
       ]
     }
     ```
     SecureString은 AWS 관리 키 `aws/ssm`으로 암호화하면 KMS 권한을 따로 줄 필요가 없다
3. **탄력적 IP**를 만들어 인스턴스에 연결
4. **DNS** (`sunsetcoast.kr`을 관리하는 곳, 예: Route 53): A 레코드 2개
   - `admin.sunsetcoast.kr` → 탄력적 IP
   - `api.sunsetcoast.kr` → 탄력적 IP
5. **S3 백업 버킷**: 기존 버킷에 `sunsetcoast-db/` 경로를 쓰거나 새 버킷(퍼블릭 액세스 차단). 수명 주기 규칙: 접두사 `sunsetcoast-db/`, 현재 버전 객체 만료 30일 (버전 관리를 켰다면 이전 버전도 30일 뒤 영구 삭제)
6. **EBS 스냅샷**: Data Lifecycle Manager로 주 1회, 4개 보관

## 2. 서버 초기 설정 (한 번)

```bash
# 시간대, 스왑 2GB (메모리 2GB 서버 여유분)
sudo timedatectl set-timezone Asia/Seoul
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Docker, AWS CLI
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu   # 다시 로그인해야 적용
sudo snap install aws-cli --classic

# 배포 파일
sudo mkdir -p /opt/sunsetcoast && sudo chown ubuntu:ubuntu /opt/sunsetcoast
git clone https://github.com/xg-r/sunsetcoast-api.git /opt/sunsetcoast
cd /opt/sunsetcoast/deploy
./fetch-env.sh                           # Parameter Store → .env (아래 3번을 먼저 등록)
```

## 3. 비밀값·설정 (SSM Parameter Store)

서버의 `deploy/.env`는 손으로 만들지 않는다. **Systems Manager → Parameter Store**에 `/sunsetcoast/prod/<이름>`으로 등록하면 `fetch-env.sh`가 읽어서 만든다 (`deploy.sh`가 배포마다 다시 받음). 표준 파라미터·`aws/ssm` 키라 비용은 없다.

파라미터 생성: 이름 `/sunsetcoast/prod/MYSQL_PASSWORD`, 계층 **표준**, 유형은 아래 표, KMS 키 소스 **내 현재 계정** · `alias/aws/ssm`, 값 입력.

| 이름 | 유형 | 값 |
|---|---|---|
| `MYSQL_ROOT_PASSWORD` | SecureString | `openssl rand -base64 24`. **처음 띄운 뒤에는 바꾸지 않는다** (DB에 저장됨) |
| `MYSQL_PASSWORD` | SecureString | 위와 같이 따로 생성. 처음 띄운 뒤 바꾸지 않는다 |
| `NAVER_CLIENT_ID` | String | 네이버 개발자센터 운영 앱 |
| `NAVER_CLIENT_SECRET` | SecureString | 〃 |
| `KAKAO_CLIENT_ID` | String | 카카오 개발자센터 운영 앱 (REST API 키) |
| `KAKAO_CLIENT_SECRET` | SecureString | 〃 |
| `AUTH_SUPER_ADMINS` | String | 첫 슈퍼어드민 (`NAVER:이메일` 또는 `KAKAO:계정번호`, 쉼표로 여러 개) |
| `ACME_EMAIL` | String | HTTPS 인증서 만료 안내를 받을 이메일 |
| `BACKUP_S3_URI` | String | `s3://<버킷>/sunsetcoast-db` |
| `API_TAG`, `WEB_TAG` | String | (선택) 없으면 `latest`. 되돌릴 때만 만든다 (6번) |

- 값에 작은따옴표(`'`)는 쓰지 않는다. 필수 값이 하나라도 없으면 `fetch-env.sh`가 실패하고 기존 `.env`를 그대로 둔다
- 값을 바꾼 뒤에는 `deploy.sh`로 다시 배포해야 반영된다
- 네이버·카카오 Redirect URI 등록:
  - `https://admin.sunsetcoast.kr/login/oauth2/code/naver`
  - `https://admin.sunsetcoast.kr/login/oauth2/code/kakao`

## 4. 실행

GHCR 이미지는 처음 빌드된 뒤 GitHub → 각 저장소 → Packages → 패키지 설정에서 **Public**으로 바꾼다 (공개 저장소라 이미지도 공개해도 된다. 비밀값은 이미지에 없다). 비공개로 두려면 서버에서 `docker login ghcr.io` (읽기 권한 토큰).

```bash
cd /opt/sunsetcoast/deploy
docker compose pull
docker compose up -d
docker compose logs -f api   # "Successfully applied N migrations", "Started" 확인
# (처음에는 백업할 데이터가 없으니 deploy.sh 대신 직접 띄운다)
```

확인:
- https://api.sunsetcoast.kr/public/health → `{"status":"UP"}`
- https://admin.sunsetcoast.kr → 로그인 화면, 네이버·카카오 로그인 후 슈퍼어드민 메뉴
- https://api.sunsetcoast.kr/settlements/coverage → 404 (관리자 API는 밖에서 막힘)

처음 띄울 때 Caddy가 인증서를 받는다. DNS가 이 서버를 가리키고 80·443이 열려 있어야 한다.

## 5. 백업

```bash
# 3일마다 04:30 DB 덤프 → S3 (매월 1·4·7…31일. 월말에는 간격이 1~2일로 짧아질 수 있다)
crontab -e
30 4 */3 * * /opt/sunsetcoast/deploy/backup.sh >> /home/ubuntu/backup.log 2>&1
```

- 보관은 S3 수명 주기 규칙(30일)으로 정한다. 3일마다 + 배포 직전(`deploy.sh`) 덤프가 쌓여 최근 한 달 치 약 10개 이상이 남는다
- 첫 실행은 직접: `./backup.sh` → S3에 `sunsetcoast-YYYYMMDD-HHMM.sql.gz`가 생기는지 확인
- 덤프에는 테이블·데이터와 로그인 기록 보호 트리거가 함께 들어간다
- **복구** (서비스를 멈추고):
  ```bash
  aws s3 cp s3://<버킷>/sunsetcoast-db/<파일>.sql.gz .
  docker compose stop api
  gunzip -c <파일>.sql.gz | docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" sunsetcoast'
  docker compose start api
  ```

## 6. 업데이트·되돌리기

`main`에 머지하면 GitHub Actions가 테스트 → 이미지 빌드 → 서버 반영까지 한다 (7번 설정 후. Actions 탭에서 확인).
서버에서 직접 반영할 때도 같은 스크립트를 쓴다:

```bash
/opt/sunsetcoast/deploy/deploy.sh        # git pull(deploy 파일) → 백업 → 전체 pull·up → api 정상 응답 확인
/opt/sunsetcoast/deploy/deploy.sh web    # 화면만 (백업 생략)
```

- DB 변경(Flyway 마이그레이션)은 api가 뜰 때 자동 적용된다. `deploy.sh`가 api를 바꾸기 전에 백업을 뜬다
- 되돌리기: Parameter Store에 `/sunsetcoast/prod/API_TAG`(또는 `WEB_TAG`)를 이전 커밋 태그(`sha-xxxxxxx`, GHCR에서 확인)로 만들고 `deploy.sh`. 다시 최신으로 갈 때는 그 파라미터를 지운다 (`.env`를 직접 고쳐도 다음 배포 때 덮어쓴다). 이미 적용된 마이그레이션은 되돌아가지 않으니 DB가 바뀐 배포는 백업에서 복구한다

## 7. GitHub Actions 자동 배포 (SSM, 한 번 설정)

Actions가 GitHub OIDC로 AWS 역할을 받아(액세스 키 저장 없음) `aws ssm send-command`로 서버의 `deploy.sh`를 실행한다. SSM 에이전트가 AWS로 먼저 연결하므로 **배포를 위해 열 인바운드 포트는 없다** (80·443은 웹 접속용).

1. **IAM → 자격 증명 공급자 → 추가**: OpenID Connect, URL `https://token.actions.githubusercontent.com`, 대상 `sts.amazonaws.com`
2. **IAM 역할** `github-deploy` (웹 자격 증명 → 위 공급자). 신뢰 정책:
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [{
       "Effect": "Allow",
       "Principal": { "Federated": "arn:aws:iam::<계정ID>:oidc-provider/token.actions.githubusercontent.com" },
       "Action": "sts:AssumeRoleWithWebIdentity",
       "Condition": {
         "StringEquals": { "token.actions.githubusercontent.com:aud": "sts.amazonaws.com" },
         "StringLike": { "token.actions.githubusercontent.com:sub": [
           "repo:xg-r/sunsetcoast-api:environment:production",
           "repo:xg-r/sunsetcoast-admin-web:environment:production"
         ] }
       }
     }]
   }
   ```
   권한 정책 (인라인):
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       { "Effect": "Allow", "Action": "ssm:SendCommand",
         "Resource": [
           "arn:aws:ec2:ap-northeast-2:<계정ID>:instance/<인스턴스ID>",
           "arn:aws:ssm:ap-northeast-2::document/AWS-RunShellScript"
         ] },
       { "Effect": "Allow", "Action": "ssm:GetCommandInvocation", "Resource": "*" }
     ]
   }
   ```
3. **두 저장소 모두** GitHub → Settings → Secrets and variables → Actions → **Variables**에 추가 (비밀값 아님):
   - `AWS_DEPLOY_ROLE_ARN` = `arn:aws:iam::<계정ID>:role/github-deploy`
   - `EC2_INSTANCE_ID` = `i-xxxxxxxxxxxxxxxxx`
4. 확인: Actions → docker → Run workflow. `deploy` job 로그에 서버의 `deploy.sh` 출력과 `status: Success`

- 변수가 없으면 `deploy` job은 건너뛴다 (이미지 빌드만)
- 배포 job은 GitHub 환경 `production`에서 돈다. Settings → Environments → production에서 승인자(Required reviewers)를 걸면 머지 후 승인해야 배포된다
- API 저장소는 `deploy.sh`(전체, 백업 포함), 화면 저장소는 `deploy.sh web`(화면만)을 실행한다. 둘이 동시에 와도 서버에서 하나씩 돈다

## 8. Savings Plan (2~4주 뒤)

처음부터 약정하지 않는다. 온디맨드로 2~4주 운영하며 메모리를 본다.

```bash
free -h                 # 스왑을 계속 많이 쓰면 메모리 부족
docker stats --no-stream
```

- 넉넉하면 `t4g.small` 그대로, 부족하면 `t4g.medium`(4GB)으로 바꾼 뒤
- **EC2 Instance Savings Plan, 1년, 선결제 없음, 서울 리전, t4g 계열**을 구매한다. 콘솔의 Savings Plans → 추천(Recommendations)이 사용량에 맞는 시간당 약정 금액을 계산해 준다
- 같은 t4g 계열 안에서는 크기를 바꿔도(small ↔ medium) 할인이 적용된다

## 9. DB 들여다보기 (DBeaver, SSH 터널)

MySQL은 서버의 `127.0.0.1:3306`에만 열려 있다. 3306을 보안 그룹에 열지 않고 SSH 터널로 접속한다.

1. 보안 그룹 인바운드: TCP 22, 소스 **내 IP** (IP가 바뀌면 다시 수정)
2. 조회 전용 계정 (한 번). 서버에서:
   ```bash
   cd /opt/sunsetcoast/deploy
   docker compose exec mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"'
   ```
   ```sql
   CREATE USER 'viewer'@'%' IDENTIFIED BY '<긴 비밀번호>';
   GRANT SELECT, SHOW VIEW ON sunsetcoast.* TO 'viewer'@'%';
   ```
   (`%`여도 밖에서는 닿지 않는다. 터널로 들어온 접속은 Docker 내부망 주소로 보인다)
3. DBeaver 새 연결 → MySQL
   - Main: Host `127.0.0.1`, Port `3306`, Database `sunsetcoast`, User `viewer`
   - Driver properties: `allowPublicKeyRetrieval=true`
   - SSH: Use SSH Tunnel, Host `<탄력적 IP>`, Port 22, User `ubuntu`, Public Key → EC2 `.pem`

데이터를 고칠 때는 화면이나 마이그레이션으로 한다. root/앱 계정으로 운영 DB를 직접 수정하지 않는다.

## 배포 전 확인 (DB 마이그레이션)

- **기존 DB로 옮겨 오는 경우** V32(이메일 유니크) 전에 같은 이메일 계정이 없어야 한다:
  `SELECT email, COUNT(*) FROM users WHERE email IS NOT NULL GROUP BY email HAVING COUNT(*) > 1;`
- V37(로그인 기록 트리거): 이 구성은 MySQL 바이너리 로그를 꺼 두어(`--skip-log-bin`) 일반 계정으로 트리거를 만들 수 있다. 바이너리 로그를 켜면 `log_bin_trust_function_creators=1`이 필요하다
- 빈 DB로 시작하면 사업자·업소·객실·객실 별칭·관리비 단가는 마이그레이션이 넣는다. 그 뒤 정산 파일을 올리면 된다
