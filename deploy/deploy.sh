#!/usr/bin/env bash
# 서버에 새 이미지를 반영한다. GitHub Actions가 SSM Run Command로 부르고, 손으로 실행해도 된다.
#   ./deploy.sh           배포 파일(git) 갱신 → 백업 → 전체 pull·up
#   ./deploy.sh web       화면만 (web 저장소에서 부를 때)
# 두 저장소의 배포가 동시에 와도 하나씩 돌도록 잠근다.
set -euo pipefail
cd "$(dirname "$0")"

exec 9>/tmp/sunsetcoast-deploy.lock
flock -w 600 9 || { echo "다른 배포가 10분 넘게 끝나지 않음"; exit 1; }

echo "== $(date) 배포 시작: ${*:-전체}"

# deploy/ 파일(compose, Caddyfile)이 바뀌었을 수 있으니 먼저 받는다
git -C .. pull --ff-only --quiet

# api가 뜨면서 DB 마이그레이션이 돌 수 있으니 배포 전에 백업한다 (화면만 바꿀 때는 생략)
if [ "$#" -eq 0 ] || printf '%s\n' "$@" | grep -qx api; then
  ./backup.sh
fi

docker compose pull --quiet "$@"
docker compose up -d --remove-orphans "$@"

# api가 다시 떴으면 정상 응답할 때까지 기다린다 (마이그레이션 포함 최대 3분)
if [ "$#" -eq 0 ] || printf '%s\n' "$@" | grep -qx api; then
  for _ in $(seq 1 36); do
    if docker compose exec -T caddy wget -q -O /dev/null http://api:8080/public/health 2>/dev/null; then
      ok=1; break
    fi
    sleep 5
  done
  if [ "${ok:-0}" != 1 ]; then
    echo "api가 3분 안에 뜨지 않음"
    docker compose logs --tail 80 api
    exit 1
  fi
fi

docker image prune -f > /dev/null
docker compose ps
echo "== $(date) 배포 완료"
