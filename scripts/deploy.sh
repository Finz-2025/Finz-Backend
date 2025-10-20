#!/bin/bash

echo ">>> build 파일 검색"
BUILD_PATH=$(ls /home/ubuntu/app/*.jar | head -n 1)
JAR_NAME=$(basename $BUILD_PATH)
echo "> build 파일명: $JAR_NAME"

echo "> build 파일 복사"
DEPLOY_PATH=/home/ubuntu/app/nonstop/jar/
mkdir -p $DEPLOY_PATH
cp $BUILD_PATH $DEPLOY_PATH

echo "> 현재 구동중인 포트 확인 (8080 or 8081)"
PORT1_HEALTH=$(curl -s http://localhost:8080/actuator/health 2>/dev/null | grep 'UP' | wc -l)
PORT2_HEALTH=$(curl -s http://localhost:8081/actuator/health 2>/dev/null | grep 'UP' | wc -l)

if [ $PORT1_HEALTH -ge 1 ]; then
  CURRENT_PORT=8080
  IDLE_PORT=8081
  IDLE_PROFILE=set2
elif [ $PORT2_HEALTH -ge 1 ]; then
  CURRENT_PORT=8081
  IDLE_PORT=8080
  IDLE_PROFILE=set1
else
  echo "> 현재 구동 중인 인스턴스가 없습니다. 기본값으로 8080을 사용합니다."
  IDLE_PORT=8080
  IDLE_PROFILE=set1
fi

echo "> application.jar 교체"
IDLE_APPLICATION=$IDLE_PROFILE-finz.jar
IDLE_APPLICATION_PATH=$DEPLOY_PATH$IDLE_APPLICATION

ln -Tfs $DEPLOY_PATH$JAR_NAME $IDLE_APPLICATION_PATH

echo "> $IDLE_PORT 에서 구동중인 애플리케이션 pid 확인"
IDLE_PID=$(pgrep -f $IDLE_APPLICATION)

if [ -z $IDLE_PID ]; then
  echo "> 현재 구동 중인 애플리케이션이 없습니다."
else
  echo "> 기존 애플리케이션 종료: kill -15 $IDLE_PID"
  kill -15 $IDLE_PID
  sleep 5
fi

echo "> $IDLE_PROFILE 배포 시작 (port: $IDLE_PORT)"
nohup java -jar $IDLE_APPLICATION_PATH --server.port=$IDLE_PORT >> /home/ubuntu/app/nohup.out 2>&1 &

echo "> $IDLE_PROFILE 10초 후 Health check 시작"
sleep 10

for retry_count in {1..10}; do
  response=$(curl -s http://localhost:$IDLE_PORT/actuator/health 2>/dev/null)
  up_count=$(echo $response | grep 'UP' | wc -l)

  if [ $up_count -ge 1 ]; then
    echo "> Health check 성공"
    break
  else
    echo "> Health check 실패 ($retry_count/10): ${response}"
  fi

  if [ $retry_count -eq 10 ]; then
    echo "> Health check 최종 실패. 배포 중단."
    exit 1
  fi

  sleep 10
done

echo "> 스위칭 실행 (port: $IDLE_PORT)"
/home/ubuntu/app/nonstop/switch.sh $IDLE_PORT
