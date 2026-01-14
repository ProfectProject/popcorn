# 📈 InfluxDB 극한 테스트 커스텀 이미지
# 다중 데이터베이스 자동 초기화

FROM influxdb:1.8-alpine

# 초기화 스크립트 복사
COPY init.sh /init.sh
RUN chmod +x /init.sh

# 커스텀 엔트리포인트 스크립트 생성
RUN cat > /custom-entrypoint.sh << 'EOF'
#!/bin/bash

# 원본 엔트리포인트 실행 (백그라운드)
/entrypoint.sh influxd &

# InfluxDB 프로세스 PID 저장
INFLUXD_PID=$!

# InfluxDB 시작 대기
echo "⏳ InfluxDB 시작 대기 중..."
sleep 10

# 초기화 스크립트 실행
echo "🚀 데이터베이스 초기화 실행..."
/init.sh

# 포그라운드에서 InfluxDB 프로세스 유지
wait $INFLUXD_PID
EOF

RUN chmod +x /custom-entrypoint.sh

# 커스텀 엔트리포인트 사용
ENTRYPOINT ["/custom-entrypoint.sh"]