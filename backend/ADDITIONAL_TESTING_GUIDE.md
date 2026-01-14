# 🚀 Popcorn 추가 테스트 실행 가이드

## 🎯 개요

극한 테스트 환경 구축 후 추가로 실행할 수 있는 다양한 테스트들을 정리한 가이드입니다.
시스템의 안정성, 성능, 보안, 가용성을 다각도로 검증할 수 있습니다.

---

## 🔧 1. 기본 환경 검증 테스트

### 1.1 애플리케이션 실행 확인
```bash
# 애플리케이션 빌드 및 실행
./gradlew bootRun

# 다른 터미널에서 헬스체크
curl http://localhost:8080/actuator/health

# 예상 결과
{"status":"UP","components":{"diskSpace":{"status":"UP"},"ping":{"status":"UP"}}}
```

### 1.2 API 기본 동작 확인
```bash
# Swagger UI 접속
open http://localhost:8080/swagger-ui/index.html

# API 테스트
curl -X GET "http://localhost:8080/api/v1/orders" \
  -H "accept: application/json"

# 메트릭 확인
curl http://localhost:8080/actuator/prometheus
```

---

## 🐒 2. Chaos Engineering 테스트

### 2.1 기본 Chaos Monkey 테스트
```bash
# Chaos Monkey 상태 확인
curl http://localhost:8080/api/v1/chaos/status

# 지연 공격 테스트 (1-5초)
curl -X POST "http://localhost:8080/api/v1/chaos/attack/latency?maxDelayMs=5000"

# 메모리 압박 테스트 (50MB)
curl -X POST "http://localhost:8080/api/v1/chaos/attack/memory?sizeMB=50"

# 예외 공격 테스트
curl -X POST "http://localhost:8080/api/v1/chaos/attack/exception?message=테스트_예외"

# 복합 공격 테스트
curl -X POST "http://localhost:8080/api/v1/chaos/attack/combo"
```

### 2.2 시나리오별 Chaos 테스트
```bash
# 🛍️ 블랙프라이데이 시나리오
curl -X POST "http://localhost:8080/api/v1/chaos/scenarios/blackfriday"

# 💾 데이터베이스 장애 시나리오
curl -X POST "http://localhost:8080/api/v1/chaos/scenarios/database-outage"

# 💳 결제 시스템 장애 시나리오
curl -X POST "http://localhost:8080/api/v1/chaos/scenarios/payment-failure"

# 🌐 네트워크 분할 시나리오
curl -X POST "http://localhost:8080/api/v1/chaos/scenarios/network-partition"

# 모든 공격 중지
curl -X POST "http://localhost:8080/api/v1/chaos/stop"
```

### 2.3 극한 모드 테스트
```bash
# 극한 모드 활성화
curl -X POST "http://localhost:8080/api/v1/chaos/extreme-mode"

# 극한 모드에서 API 호출 테스트
for i in {1..10}; do
  curl -w "Response time: %{time_total}s\n" \
    http://localhost:8080/api/v1/chaos/status
  sleep 2
done

# 통계 확인
curl http://localhost:8080/api/v1/chaos/stats
```

---

## 🌊 3. 극한 성능 테스트

### 3.1 동시 접속 테스트
```bash
# 5백만명 동시 접속 시뮬레이션
curl -X POST "http://localhost:8080/api/v1/extreme/five-million-users?enableChaos=true"

# 1만명 동시 결제 테스트
curl -X POST "http://localhost:8080/api/v1/extreme/ten-thousand-payments"

# 24시간 지속 테스트 상태 확인
curl "http://localhost:8080/api/v1/extreme/endurance-test-status"

# 메모리 누수 탐지
curl -X POST "http://localhost:8080/api/v1/extreme/memory-leak-detection"
```

### 3.2 네트워크 및 CPU 테스트
```bash
# 네트워크 지연 시뮬레이션 (200ms)
curl -X POST "http://localhost:8080/api/v1/extreme/network-delay-simulation?delayMs=200"

# CPU 집약적 작업 (500만 반복)
curl -X POST "http://localhost:8080/api/v1/extreme/cpu-intensive-simulation?iterations=5000000"

# 종합 성능 리포트
curl "http://localhost:8080/api/v1/extreme/performance-report"
```

---

## 🐳 4. Docker 기반 통합 테스트

### 4.1 기본 Docker Compose 실행
```bash
# 기본 환경 시작
docker-compose -f docker-compose.extreme.yml up -d

# 서비스 상태 확인
docker-compose -f docker-compose.extreme.yml ps

# 로그 확인
docker-compose -f docker-compose.extreme.yml logs -f popcorn-backend
```

### 4.2 프로파일별 테스트 실행
```bash
# K6 테스트만 실행
docker-compose -f docker-compose.extreme.yml --profile k6 up k6-extreme

# JMeter 테스트만 실행
docker-compose -f docker-compose.extreme.yml --profile jmeter up jmeter-extreme

# 모든 프로파일 동시 실행 (극한 테스트)
docker-compose -f docker-compose.extreme.yml --profile k6 --profile jmeter up -d
```

---

## 📊 5. 모니터링 및 메트릭 확인

### 5.1 Grafana 대시보드
```bash
# Grafana 접속
open http://localhost:3000
# 로그인: admin / extreme_admin

# 사전 설정된 대시보드 확인:
# - 🚀 Popcorn 극한 테스트 대시보드
# - 동시 사용자 수
# - 응답 시간 (P95/P99)
# - 요청 처리량 (RPS)
# - 오류율
# - 메모리 및 CPU 사용량
```

### 5.2 Prometheus 메트릭
```bash
# Prometheus 접속
open http://localhost:9091

# 주요 메트릭 쿼리:
# - http_server_requests_seconds_count (총 요청 수)
# - http_server_requests_seconds_bucket (응답 시간 분포)
# - jvm_memory_used_bytes (JVM 메모리 사용량)
# - process_cpu_usage (CPU 사용률)
```

### 5.3 애플리케이션 메트릭
```bash
# Spring Boot Actuator 메트릭
curl http://localhost:8080/actuator/metrics

# JVM 메트릭
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# HTTP 메트릭
curl http://localhost:8080/actuator/metrics/http.server.requests

# 데이터베이스 커넥션 풀
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

---

## 🎯 6. Gradle 기반 테스트 실행

### 6.1 기본 테스트 스위트
```bash
# 일반 단위 테스트
./gradlew test

# 통합 테스트
./gradlew integrationTest

# 전체 테스트 (단위 + 통합)
./gradlew check
```

### 6.2 부하 테스트 스위트
```bash
# 기본 JMeter 테스트
./gradlew jmeterTest

# 백만 사용자 JMeter 테스트
./gradlew jmeterMillionTest

# K6 기본 테스트
./gradlew k6Test

# K6 극한 테스트 (5백만명)
./gradlew k6UltimateTest

# 모든 극한 테스트 통합 실행
./gradlew ultimateLoadTest
```

---

## 🧪 7. 대화형 테스트 스크립트

### 7.1 자동화된 테스트 메뉴
```bash
# 대화형 극한 테스트 메뉴 실행
./scripts/run-extreme-tests.sh

# 메뉴 옵션:
# 1. 🌊 5백만명 동시 접속 테스트 (K6)
# 2. 💳 1만명 동시 결제 테스트 (JMeter)
# 3. ⏰ 24시간 지속 부하 테스트
# 4. 🐒 Chaos Engineering 통합 테스트
# 5. 🚀 ULTIMATE 모든 테스트 동시 실행
# 6. 📊 모니터링 대시보드만 실행
# 7. 🛑 모든 테스트 중지 및 정리
```

---

## 🔍 8. 결과 분석 및 리포트

### 8.1 테스트 결과 위치
```bash
# 테스트 결과 디렉토리 확인
ls -la build/reports/

# JMeter 결과
open build/reports/jmeter/html/index.html

# K6 결과 (JSON)
cat build/reports/k6/extreme-results.json | jq .

# 성능 테스트 리포트
open build/reports/stress/index.html
```

### 8.2 로그 분석
```bash
# 애플리케이션 로그
docker logs popcorn-backend-extreme

# JVM 로그
tail -f monitoring/logs/jvm.log

# 모든 컨테이너 로그
docker-compose -f docker-compose.extreme.yml logs -f
```

---

## 📈 9. 성능 기준 및 임계값

### 9.1 성공 기준
- **응답 시간**: P95 < 1초, P99 < 3초
- **처리량**: > 50,000 req/s
- **오류율**: < 5%
- **메모리 사용률**: < 90%
- **복구 시간**: < 30초

### 9.2 알람 임계값
```bash
# 응답 시간 임계값 초과 확인
curl "http://localhost:9091/api/v1/query?query=histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 1"

# 오류율 임계값 초과 확인
curl "http://localhost:9091/api/v1/query?query=rate(http_server_requests_seconds_count{status=~\"5..\"}[5m]) / rate(http_server_requests_seconds_count[5m]) > 0.05"

# 메모리 사용률 임계값 초과 확인
curl "http://localhost:9091/api/v1/query?query=jvm_memory_used_bytes{area=\"heap\"} / jvm_memory_max_bytes{area=\"heap\"} > 0.9"
```

---

## 🛡️ 10. 장애 복구 테스트

### 10.1 서비스 복구 테스트
```bash
# 서비스 강제 중단
docker stop popcorn-backend-extreme

# 복구 시간 측정
time docker start popcorn-backend-extreme

# 헬스체크로 복구 확인
while ! curl -f http://localhost:8080/actuator/health; do
  echo "서비스 복구 대기 중..."
  sleep 5
done
echo "서비스 복구 완료!"
```

### 10.2 데이터베이스 복구 테스트
```bash
# DB 강제 중단
docker stop postgres-extreme

# 애플리케이션 동작 확인 (DB 없이)
curl http://localhost:8080/api/v1/orders

# DB 복구
docker start postgres-extreme

# 연결 복구 확인
curl http://localhost:8080/actuator/health
```

---

## 🎪 11. 종합 시나리오 테스트

### 11.1 현실적인 트래픽 패턴 시뮬레이션
```bash
#!/bin/bash
# realistic-traffic.sh

echo "🌅 오전 피크 시간대 시뮬레이션"
curl -X POST "http://localhost:8080/api/v1/extreme/five-million-users?enableChaos=false"
sleep 300

echo "🍽️ 점심 시간대 + Chaos 테스트"
curl -X POST "http://localhost:8080/api/v1/chaos/scenarios/blackfriday" &
curl -X POST "http://localhost:8080/api/v1/extreme/ten-thousand-payments"
sleep 600

echo "🌙 저녁 피크 + 극한 모드"
curl -X POST "http://localhost:8080/api/v1/chaos/extreme-mode"
curl -X POST "http://localhost:8080/api/v1/extreme/cpu-intensive-simulation?iterations=1000000"
sleep 900

echo "🛑 테스트 종료 및 정리"
curl -X POST "http://localhost:8080/api/v1/chaos/stop"
```

### 11.2 비즈니스 로직 스트레스 테스트
```bash
# 주문 처리 스트레스 테스트
for i in {1..1000}; do
  curl -X POST "http://localhost:8080/api/v1/orders" \
    -H "Content-Type: application/json" \
    -d '{"storeId":"test-store","items":[{"goodsId":"test-goods","quantity":1}]}' &
done

# 결제 처리 스트레스 테스트
for i in {1..500}; do
  curl -X POST "http://localhost:8080/api/v1/payments" \
    -H "Content-Type: application/json" \
    -d '{"orderId":"test-order","amount":10000}' &
done

wait
echo "모든 요청 완료"
```

---

## 🚀 12. 최종 검증 체크리스트

### 12.1 기능 검증
- [ ] 모든 API 엔드포인트 정상 동작
- [ ] 인증/인가 시스템 정상 동작
- [ ] 데이터베이스 CRUD 정상 동작
- [ ] 파일 업로드/다운로드 정상 동작

### 12.2 성능 검증
- [ ] 5백만명 동시 접속 처리
- [ ] 1백만건 동시 주문 처리
- [ ] 1만명 동시 결제 처리
- [ ] 24시간 지속 운영

### 12.3 안정성 검증
- [ ] Chaos Engineering 모든 시나리오 통과
- [ ] 메모리 누수 없음
- [ ] CPU 사용률 정상
- [ ] 에러 복구 시간 < 30초

### 12.4 모니터링 검증
- [ ] Grafana 대시보드 정상 표시
- [ ] Prometheus 메트릭 수집 정상
- [ ] 알람 시스템 정상 동작
- [ ] 로그 수집 및 분석 가능

---

## 🎯 13. 추천 테스트 시퀀스

### 13.1 일일 테스트 (30분)
```bash
# 1. 기본 환경 확인 (5분)
curl http://localhost:8080/actuator/health
./gradlew compileJava

# 2. 기본 부하 테스트 (10분)
./gradlew jmeterTest

# 3. 간단한 Chaos 테스트 (15분)
curl -X POST "http://localhost:8080/api/v1/chaos/attack/latency?maxDelayMs=2000"
curl -X POST "http://localhost:8080/api/v1/chaos/attack/memory?sizeMB=50"
curl -X POST "http://localhost:8080/api/v1/chaos/stop"
```

### 13.2 주간 테스트 (2시간)
```bash
# 1. 전체 빌드 및 테스트 (30분)
./gradlew clean build

# 2. 극한 부하 테스트 (60분)
./gradlew ultimateLoadTest

# 3. 전체 Chaos 시나리오 (30분)
./scripts/run-extreme-tests.sh
# 메뉴에서 4번 선택 (Chaos Engineering 통합 테스트)
```

### 13.3 월간 테스트 (24시간)
```bash
# 1. 24시간 지속 테스트 시작
./scripts/run-extreme-tests.sh
# 메뉴에서 3번 선택 (24시간 지속 부하 테스트)

# 2. 모니터링 대시보드 24시간 관찰
# - Grafana: http://localhost:3000
# - 임계값 초과 시 알람 확인
# - 메모리 누수 패턴 분석
# - 성능 저하 구간 분석

# 3. 최종 종합 리포트 생성
curl "http://localhost:8080/api/v1/extreme/performance-report"
```

---

## 🔧 14. 문제 해결 가이드

### 14.1 일반적인 문제들
```bash
# 포트 충돌 해결
sudo lsof -i :8080
kill -9 <PID>

# Docker 리소스 정리
docker-compose -f docker-compose.extreme.yml down
docker system prune -a

# 메모리 부족 시
export JAVA_OPTS="-Xms4g -Xmx16g"
```

### 14.2 성능 튜닝
```bash
# JVM 튜닝
export JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:MaxGCPauseMillis=100"

# 데이터베이스 최적화
docker exec postgres-extreme psql -U popcorn -c "ANALYZE;"

# 시스템 리소스 확인
htop
free -h
df -h
```

---

**🎉 이제 준비 완료! 극한 테스트로 시스템의 한계를 뛰어넘어보세요! 🚀**

> **💡 팁**: 테스트는 단계별로 진행하며, 각 단계의 결과를 분석한 후 다음 단계로 진행하는 것이 좋습니다.