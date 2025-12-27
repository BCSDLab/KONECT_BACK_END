#!/bin/bash
set -euo pipefail

NGINX_CONF="/etc/nginx/conf.d/konect.conf"

health_check() {
  curl -fsS --connect-timeout 2 --max-time 3 "http://localhost:$1/actuator/health" | grep -q '"status":"UP"'
}

ACTIVE_PORT=""
if [ -f "$NGINX_CONF" ]; then
  ACTIVE_PORT=$(grep -Eo 'proxy_pass http://(localhost|127\.0\.0\.1):[0-9]+' "$NGINX_CONF" | head -n 1 | grep -Eo '[0-9]+$' || true)
fi

PORT_8080_UP=0
PORT_8081_UP=0
if health_check 8080; then
  PORT_8080_UP=1
fi
if health_check 8081; then
  PORT_8081_UP=1
fi

if [ "$ACTIVE_PORT" != "8080" ] && [ "$ACTIVE_PORT" != "8081" ]; then
  if [ "$PORT_8080_UP" -eq 1 ] && [ "$PORT_8081_UP" -eq 0 ]; then
    ACTIVE_PORT=8080
  elif [ "$PORT_8080_UP" -eq 0 ] && [ "$PORT_8081_UP" -eq 1 ]; then
    ACTIVE_PORT=8081
  else
    echo "Unable to determine active port (nginx: $ACTIVE_PORT, 8080 up=$PORT_8080_UP, 8081 up=$PORT_8081_UP)"
    exit 1
  fi
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
ready=0
for i in {1..20}; do
  if health_check "$INACTIVE_PORT"; then
    ready=1
    break
  fi
  sleep 3
done

if [ "$ready" -ne 1 ]; then
  echo "New version failed to start"
  sudo systemctl stop konect-$INACTIVE.service
  exit 1
fi

echo "Switching Nginx to $INACTIVE_PORT..."
NGINX_BACKUP="${NGINX_CONF}.bak.$(date +%s)"
sudo cp "$NGINX_CONF" "$NGINX_BACKUP"
sudo sed -i -E "s@(proxy_pass http://(localhost|127\\.0\\.0\\.1)):${ACTIVE_PORT}@\\1:${INACTIVE_PORT}@g" "$NGINX_CONF"
if ! grep -Eq "proxy_pass http://(localhost|127\\.0\\.0\\.1):${INACTIVE_PORT}" "$NGINX_CONF"; then
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
if ! sudo nginx -s reload; then
  echo "Nginx reload failed, rolling back"
  sudo mv "$NGINX_BACKUP" "$NGINX_CONF"
  sudo nginx -t || true
  sudo nginx -s reload || true
  sudo systemctl stop konect-$INACTIVE.service
  exit 1
fi
sudo rm -f "$NGINX_BACKUP"

echo "Stopping old service: konect-$ACTIVE"
sudo systemctl stop konect-$ACTIVE.service

echo "Blue-Green deployment completed successfully."
