#!/bin/bash

IDLE_PORT=$1

if [ -z "$IDLE_PORT" ]; then
  echo "[switch.sh] 포트 인자가 필요합니다."
  exit 1
fi

echo "Nginx 포트를 $IDLE_PORT 로 스위칭 중..."
echo "set \$service_url http://127.0.0.1:${IDLE_PORT};" | sudo tee /etc/nginx/conf.d/service-url.inc

echo "Nginx Reload 중..."
sudo service nginx reload

echo "Nginx Reload 완료. 현재 포트: $IDLE_PORT"
