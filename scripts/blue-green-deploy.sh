#!/bin/bash
set -euo pipefail

if curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"'; then
  ACTIVE=blue
  INACTIVE=green
  ACTIVE_PORT=8080
  INACTIVE_PORT=8081
else
  ACTIVE=green
  INACTIVE=blue
  ACTIVE_PORT=8081
  INACTIVE_PORT=8080
fi

echo "Active=$ACTIVE ($ACTIVE_PORT), Inactive=$INACTIVE ($INACTIVE_PORT)"

echo "Starting inactive service: konect-$INACTIVE"
sudo systemctl start konect-$INACTIVE.service

echo "Waiting for new version on port $INACTIVE_PORT..."
ready=0
for i in {1..20}; do
  if curl -fsS http://localhost:$INACTIVE_PORT/actuator/health | grep -q '"status":"UP"'; then
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
sudo sed -i "s/$ACTIVE_PORT/$INACTIVE_PORT/" /etc/nginx/conf.d/konect.conf
sudo nginx -s reload

echo "Stopping old service: konect-$ACTIVE"
sudo systemctl stop konect-$ACTIVE.service

echo "Blue-Green deployment completed successfully."
