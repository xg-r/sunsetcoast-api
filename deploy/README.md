# 운영 배포 (AWS EC2 + Docker Compose)

HTTPS는 ALB(ACM 인증서)가 맡고, 뒤의 EC2 한 대에 Caddy · 관리자 화면 · API · MySQL을 모두 올린다.

```
admin.sunsetcoast.kr ─┐                                         ┌→ web (Next.js) ──(내부망 /api)──→ api
                      ├→ ALB (443, ACM) ─HTTP:80→ EC2 ─ Caddy ─┤
api.sunsetcoast.kr  ──┘                                         └→ api 의 /public/**, /me/** 만 ──→ api → mysql
sunsetcoast.kr → 지금처럼 S3 + CloudFront (이 서버와 무관)
```

- 밖에서는 ALB만 닿는다. EC2는 ALB에서 오는 80만 받고(보안 그룹), web·api·mysql은 Docker 내부망에만 있다.
- Caddy는 ALB가 붙인 `X-Forwarded-*` 헤더를 그대로 넘긴다(`trusted_proxies`). 그래야 OAuth redirect_uri가 `https://`로 만들어지고 로그인 기록에 실제 접속 IP가 남는다.
- `api.sunsetcoast.kr`에서는 사용자용 경로(`/public/**`, `/me/**`)만 통과한다. 정산·관리비·계좌 같은 관리자 API는 404이고, 관리자 화면 서버를 거쳐서만 닿는다.
- 이미지는 GitHub Actions가 `main`에 올라올 때마다 ARM용으로 빌드해 GHCR(`ghcr.io/xg-r/sunsetcoast-api`, `ghcr.io/xg-r/sunsetcoast-admin-web`)에 올리고, SSM Run Command로 서버에서 `deploy.sh`를 실행해 반영한다 (EC2 인바운드 포트 불필요, 7번).

## 비용 (서울 리전, 대략)

| 항목 | 온디맨드 | Savings Plan 1년 후 |
|---|---|---|
| EC2 t4g.small (ARM, 2 vCPU, 2GB) | 약 $15/월 | 약 $9~10/월 |
| EC2 공인 IPv4 (이미지 pull·백업용 아웃바운드) | 약 $3.6/월 | 약 $3.6/월 |
| ALB (기본 요금 + LCU, 트래픽 적음) | 약 $17~20/월 | 할인 없음 |
| ALB 공인 IPv4 (AZ 2개) | 약 $7.3/월 | 할인 없음 |
| EBS gp3 30GB + 주간 스냅샷 | 약 $3/월 | 약 $3/월 |
| S3 백업 (30일 보관) | 몇십 원 | 몇십 원 |

정확한 금액은 [AWS 요금 계산기](https://calculator.aws/)로 확인한다. 로컬에서 같은 구성을 띄워 본 메모리 사용량: api 약 500MB, mysql 약 230MB, web 약 40MB, caddy 약 10MB.

## 1. AWS 준비 (서울 리전 `ap-northeast-2`)

1. **ACM 인증서**: `*.sunsetcoast.kr` 요청 → DNS 검증 → 인증서 화면의 "Route 53에서 레코드 생성". 검증 CNAME은 지우지 않는다(자동 갱신)
2. **보안 그룹 2개**
   - `sunsetcoast-alb-sg`: 인바운드 TCP 80, 443 (0.0.0.0/0, ::/0)
   - `sunsetcoast-ec2-sg`: 인바운드 TCP 80, 소스 = `sunsetcoast-alb-sg`. 그 밖에는 열지 않는다 (SSH 대신 Session Manager)
3. **IAM 역할** (인스턴스 프로필, 예: `sunsetcoast-ec2`)
   - `AmazonSSMManagedInstanceCore` (Session Manager 접속, GitHub Actions 배포)
   - 백업 버킷 경로에 쓰기만: `s3:PutObject` on `arn:aws:s3:::<버킷>/sunsetcoast-db/*`
4. **EC2 인스턴스**
   - AMI: Ubuntu Server 24.04 LTS (**arm64**), 유형 `t4g.small`, gp3 30GB
   - 퍼블릭 서브넷, 퍼블릭 IP 자동 할당 켬 (이미지 pull·S3·OAuth 호출용 아웃바운드. 탄력적 IP는 필요 없다)
   - 보안 그룹 `sunsetcoast-ec2-sg`, IAM 역할 `sunsetcoast-ec2`
5. **대상 그룹** `sunsetcoast-tg`: 인스턴스, HTTP 80, HTTP1, 위 인스턴스 등록
   - 헬스체크: 경로 `/public/health`, 성공 코드 `200`, 정상/비정상 임계값 2/3, 제한 시간 5초, 간격 15초
   - 속성 → 등록 취소 지연 30초
6. **ALB** `sunsetcoast-alb`: Internet-facing, IPv4, 서로 다른 AZ의 퍼블릭 서브넷 2개 이상(EC2가 있는 AZ 포함), 보안 그룹 `sunsetcoast-alb-sg`만
   - 리스너 HTTPS:443 → `sunsetcoast-tg`로 전달, 보안 정책 `ELBSecurityPolicy-TLS13-1-2-2021-06`, 인증서 `*.sunsetcoast.kr`
   - 리스너 HTTP:80 → HTTPS:443으로 리디렉션 (301)
   - 속성: 잘못된 헤더 필드 삭제 켬, 삭제 방지 켬
7. **Route 53** (호스팅 영역 `sunsetcoast.kr`): A 레코드 2개, **별칭** → Application Load Balancer → 서울 → `sunsetcoast-alb`
   - `admin.sunsetcoast.kr`, `api.sunsetcoast.kr`
8. **S3 백업 버킷**: 기존 버킷에 `sunsetcoast-db/` 경로를 쓰거나 새 버킷(퍼블릭 액세스 차단). 수명 주기 규칙: `sunsetcoast-db/` 아래 30일 뒤 삭제
9. **EBS 스냅샷**: Data Lifecycle Manager로 주 1회, 4개 보관

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
cp .env.example .env && chmod 600 .env   # 값 채우기 (아래 3번)
```

## 3. 비밀값 (`deploy/.env`)

`.env.example`의 항목을 채운다. git에 올리지 않는다 (`.gitignore`).

- `MYSQL_ROOT_PASSWORD`, `MYSQL_PASSWORD`: `openssl rand -base64 24`로 만든 긴 값. **처음 띄운 뒤에는 바꾸지 않는다** (DB에 저장됨)
- `NAVER_*`, `KAKAO_*`: 개발자센터의 운영 앱 키. Redirect URI 등록:
  - `https://admin.sunsetcoast.kr/login/oauth2/code/naver`
  - `https://admin.sunsetcoast.kr/login/oauth2/code/kakao`
- `AUTH_SUPER_ADMINS`: 첫 슈퍼어드민 (`NAVER:이메일` 또는 `KAKAO:계정번호`)
- `BACKUP_S3_URI`: `s3://<버킷>/sunsetcoast-db`

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
- EC2 → 대상 그룹 `sunsetcoast-tg` → 대상이 **healthy**
- https://api.sunsetcoast.kr/public/health → `{"status":"UP"}`
- https://admin.sunsetcoast.kr → 로그인 화면, 네이버·카카오 로그인 후 슈퍼어드민 메뉴
- https://api.sunsetcoast.kr/settlements/coverage → 404 (관리자 API는 밖에서 막힘)

- 관리자 화면에서 로그인한 뒤 슈퍼어드민의 로그인 기록에 ALB 주소(10.x, 172.x)가 아닌 실제 접속 IP가 찍히는지

## 5. 백업

```bash
# 매일 04:30 DB 덤프 → S3
crontab -e
30 4 * * * /opt/sunsetcoast/deploy/backup.sh >> /home/ubuntu/backup.log 2>&1
```

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
- 되돌리기: `.env`의 `API_TAG`/`WEB_TAG`를 이전 커밋 태그(`sha-xxxxxxx`, GHCR에서 확인)로 바꾸고 `docker compose up -d`. 이미 적용된 마이그레이션은 되돌아가지 않으니 DB가 바뀐 배포는 백업에서 복구한다

## 7. GitHub Actions 자동 배포 (SSM, 한 번 설정)

Actions가 GitHub OIDC로 AWS 역할을 받아(액세스 키 저장 없음) `aws ssm send-command`로 서버의 `deploy.sh`를 실행한다. SSM 에이전트가 AWS로 먼저 연결하므로 **EC2 인바운드 포트는 열지 않는다**.

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

- ALB에는 Savings Plan 할인이 없다
- 넉넉하면 `t4g.small` 그대로, 부족하면 `t4g.medium`(4GB)으로 바꾼 뒤
- **EC2 Instance Savings Plan, 1년, 선결제 없음, 서울 리전, t4g 계열**을 구매한다. 콘솔의 Savings Plans → 추천(Recommendations)이 사용량에 맞는 시간당 약정 금액을 계산해 준다
- 같은 t4g 계열 안에서는 크기를 바꿔도(small ↔ medium) 할인이 적용된다

## 배포 전 확인 (DB 마이그레이션)

- **기존 DB로 옮겨 오는 경우** V32(이메일 유니크) 전에 같은 이메일 계정이 없어야 한다:
  `SELECT email, COUNT(*) FROM users WHERE email IS NOT NULL GROUP BY email HAVING COUNT(*) > 1;`
- V37(로그인 기록 트리거): 이 구성은 MySQL 바이너리 로그를 꺼 두어(`--skip-log-bin`) 일반 계정으로 트리거를 만들 수 있다. 바이너리 로그를 켜면 `log_bin_trust_function_creators=1`이 필요하다
- 빈 DB로 시작하면 사업자·업소·객실·객실 별칭·관리비 단가는 마이그레이션이 넣는다. 그 뒤 정산 파일을 올리면 된다
