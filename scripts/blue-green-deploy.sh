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
  echo "Both ports UP — assuming 8080 active"
  ACTIVE_PORT=8080
else
  echo "Both ports DOWN — abort"
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

echo "Active=$ACTIVE ($ACTIVE_PORT), Inactive=$INACTIVE ($INACTIVE_PORT)"

echo "Starting inactive service: konect-$INACTIVE"
sudo systemctl start konect-$INACTIVE.service

echo "Waiting for new version on port $INACTIVE_PORT..."
for i in {1..20}; do
  health_check "$INACTIVE_PORT" && break
  sleep 3
  if [ "$i" -eq 20 ]; then
    echo "New version failed to start"
    sudo systemctl stop konect-$INACTIVE.service
    exit 1
  fi
done

echo "Switching Nginx to $INACTIVE_PORT..."
NGINX_BACKUP="${NGINX_CONF}.bak.$(date +%s)"
sudo cp "$NGINX_CONF" "$NGINX_BACKUP"

sudo sed -i -E "s@(server 127\.0\.0\.1:)${ACTIVE_PORT}@\1${INACTIVE_PORT}@g" "$NGINX_CONF"

if ! grep -Eq "server 127\.0\.0\.1:${INACTIVE_PORT}" "$NGINX_CONF"; then
  echo "Failed to update Nginx config"
  sudo mv "$NGINX_BACKUP" "$NGINX_CONF"
  sudo systemctl stop konect-$INACTIVE.service
  exit 1
fi

if ! sudo nginx -t; then
  echo "Nginx config test failed, rolling back"
  sudo mv "$NGINX_BACKUP" "$NGINX_CONF"
  sudo systemctl stop konect-$INACTIVE.service
  exit 1
fi

sudo nginx -s reload
sudo rm -f "$NGINX_BACKUP"

echo "Stopping old service: konect-$ACTIVE"
sudo systemctl stop konect-$ACTIVE.service

echo "Blue-Green deployment completed successfully."
