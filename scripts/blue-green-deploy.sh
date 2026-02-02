#!/bin/bash
set -euo pipefail

NGINX_CONF="/etc/nginx/conf.d/konect.conf"

health_check() {
  curl -fsS --connect-timeout 2 --max-time 3 "http://localhost:$1/actuator/health" | grep -q '"status":"UP"' || return 1
}

PORT_8080_UP=0
PORT_8081_UP=0
health_check 8080 && PORT_8080_UP=1 || true
health_check 8081 && PORT_8081_UP=1 || true

if [ "$PORT_8080_UP" -eq 1 ] && [ "$PORT_8081_UP" -eq 0 ]; then
  ACTIVE_PORT=8080
elif [ "$PORT_8080_UP" -eq 0 ] && [ "$PORT_8081_UP" -eq 1 ]; then
  ACTIVE_PORT=8081
elif [ "$PORT_8080_UP" -eq 1 ] && [ "$PORT_8081_UP" -eq 1 ]; then
  echo "[경고] 두 포트 모두 살아 있음 → 8080을 활성 포트로 가정합니다."
  ACTIVE_PORT=8080
else
  echo "[오류] 두 포트 모두 내려가 있음 → 배포 중단"
  exit 1
fi

if [ "$ACTIVE_PORT" = "8080" ]; then
  ACTIVE=blue
  INACTIVE=green
  INACTIVE_PORT=8081
else
  ACTIVE=green
  INACTIVE=blue
  INACTIVE_PORT=8080
fi

echo "--------------------------------------------------"
echo " 현재 상태"
echo "  - 활성 서비스   : $ACTIVE ($ACTIVE_PORT)"
echo "  - 비활성 서비스 : $INACTIVE ($INACTIVE_PORT)"
echo "--------------------------------------------------"

echo "[1/5] 비활성 서비스 시작 → konect-$INACTIVE"
sudo systemctl start konect-$INACTIVE.service

echo "[2/5] 새 버전 헬스체크 대기 (포트 $INACTIVE_PORT)..."
for i in {1..20}; do
  if health_check "$INACTIVE_PORT"; then
    echo "      헬스체크 통과"
    break
  fi
  sleep 3
  if [ "$i" -eq 20 ]; then
    echo "[오류] 새 버전이 정상 기동되지 않음 → 롤백"
    sudo systemctl stop konect-$INACTIVE.service
    exit 1
  fi
done

echo "[3/5] Nginx 트래픽 전환 → 포트 $INACTIVE_PORT"
NGINX_BACKUP="${NGINX_CONF}.bak.$(date +%s)"
sudo cp "$NGINX_CONF" "$NGINX_BACKUP"

sudo sed -i -E "s@(server 127\.0\.0\.1:)${ACTIVE_PORT}@\1${INACTIVE_PORT}@g" "$NGINX_CONF"

if ! grep -Eq "server 127\.0\.0\.1:${INACTIVE_PORT}" "$NGINX_CONF"; then
  echo "[오류] Nginx 설정 수정 실패 → 롤백"
  sudo mv "$NGINX_BACKUP" "$NGINX_CONF"
  sudo systemctl stop konect-$INACTIVE.service
  exit 1
fi

if ! sudo nginx -t; then
  echo "[오류] Nginx 설정 검증 실패 → 롤백"
  sudo mv "$NGINX_BACKUP" "$NGINX_CONF"
  sudo systemctl stop konect-$INACTIVE.service
  exit 1
fi

sudo nginx -s reload
sudo rm -f "$NGINX_BACKUP"

echo "[4/5] 기존 활성 서비스 종료 → konect-$ACTIVE"
sudo systemctl stop konect-$ACTIVE.service

echo "[5/5] 배포 완료"
echo "--------------------------------------------------"
echo " 블루-그린 배포가 성공적으로 완료되었습니다."
echo "--------------------------------------------------"
