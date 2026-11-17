#!/usr/bin/env bash
# AWS SSM Parameter Store의 /sunsetcoast/prod/* 를 읽어 deploy/.env 를 만든다.
#   파라미터 이름의 마지막 부분이 변수 이름이 된다 (/sunsetcoast/prod/MYSQL_PASSWORD → MYSQL_PASSWORD).
#   EC2 IAM 역할에 ssm:GetParametersByPath 권한이 필요하다 (README 1번). SecureString은 aws/ssm 기본 키로 복호화.
# deploy.sh가 매번 부른다. 처음 띄울 때는 직접: ./fetch-env.sh
set -euo pipefail
cd "$(dirname "$0")"

PREFIX="${SSM_PREFIX:-/sunsetcoast/prod}"
REGION="${AWS_REGION:-ap-northeast-2}"
REQUIRED=(MYSQL_ROOT_PASSWORD MYSQL_PASSWORD NAVER_CLIENT_ID NAVER_CLIENT_SECRET KAKAO_CLIENT_ID KAKAO_CLIENT_SECRET AUTH_SUPER_ADMINS ACME_EMAIL BACKUP_S3_URI)

tmp="$(mktemp ./.env.XXXXXX)"
trap 'rm -f "$tmp"' EXIT
chmod 600 "$tmp"

rows="$(aws ssm get-parameters-by-path --region "$REGION" --path "$PREFIX" --recursive --with-decryption \
  --query 'Parameters[].[Name,Value]' --output text)"

{
  echo "# $(date '+%F %T') ${PREFIX} 에서 fetch-env.sh로 만듦. 직접 고치지 말고 Parameter Store에서 바꾼다"
  while IFS=$'\t' read -r name value; do
    [ -n "$name" ] || continue
    key="${name##*/}"
    [[ "$key" =~ ^[A-Z_][A-Z0-9_]*$ ]] || { echo "건너뜀: 변수 이름으로 쓸 수 없는 파라미터 $name" >&2; continue; }
    # 작은따옴표로 감싸 문자 그대로 쓴다 (compose .env와 bash source 모두 그대로 읽음). 값에 작은따옴표는 쓰지 않는다
    [[ "$value" != *"'"* ]] || { echo "$name 값에 작은따옴표(')가 있어 쓸 수 없음" >&2; exit 1; }
    printf "%s='%s'\n" "$key" "$value"
  done <<< "$rows"
} > "$tmp"

missing=()
for k in "${REQUIRED[@]}"; do
  grep -q "^${k}=" "$tmp" || missing+=("$k")
done
if [ "${#missing[@]}" -gt 0 ]; then
  echo "Parameter Store(${PREFIX})에 없는 값: ${missing[*]} — .env를 바꾸지 않음" >&2
  exit 1
fi

mv "$tmp" .env
trap - EXIT
echo "$(date) .env 갱신: $(($(wc -l < .env) - 1))개 (${PREFIX})"
