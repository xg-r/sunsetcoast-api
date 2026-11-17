#!/usr/bin/env bash
# 3일마다 DB를 덤프해서 S3에 올린다. cron 예: 30 4 */3 * * /opt/sunsetcoast/deploy/backup.sh >> /var/log/sunsetcoast-backup.log 2>&1
# 보관 기간은 S3 수명 주기 규칙으로 정한다 (30일 뒤 삭제). EC2 IAM 역할에 그 경로 s3:PutObject 권한이 필요하다
set -euo pipefail
cd "$(dirname "$0")"
set -a; source ./.env; set +a

stamp="$(TZ=Asia/Seoul date +%Y%m%d-%H%M)"
file="/tmp/sunsetcoast-${stamp}.sql.gz"

# --single-transaction: 서비스를 멈추지 않고 일관된 덤프. 트리거(로그인 기록 보호)도 함께 담는다
docker compose exec -T mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers --set-gtid-purged=OFF sunsetcoast' \
  | gzip -9 > "$file"

# 덤프가 비어 있으면 실패로 본다
[ "$(gzip -dc "$file" | head -c 1000 | wc -c)" -gt 100 ] || { echo "$(date) 덤프 실패: 내용 없음"; rm -f "$file"; exit 1; }

aws s3 cp "$file" "${BACKUP_S3_URI}/sunsetcoast-${stamp}.sql.gz" --only-show-errors
rm -f "$file"
echo "$(date) 백업 완료: ${BACKUP_S3_URI}/sunsetcoast-${stamp}.sql.gz"
