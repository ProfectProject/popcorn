# 🚀 Popcorn 극한 테스트 가이드

## 🌊 개요

Popcorn 백엔드 시스템의 극한 성능을 테스트하기 위한 통합 환경입니다.

### 🎯 지원하는 극한 테스트
- **5,000,000명 동시 접속** (K6)
- **1,000,000명 동시 주문** (JMeter)
- **10,000명 동시 결제** (실시간)
- **24시간 지속 부하** (내구성)
- **Chaos Engineering** (장애 복구)

---

## 🔥 빠른 시작

### 1단계: 시스템 요구사항 확인
```bash
# 권장 사양
- RAM: 16GB+ (극한 테스트시 32GB+)
- CPU: 8코어+ (극한 테스트시 16코어+)
- Docker: 설치됨
- Docker Compose: 설치됨
```

### 2단계: Docker 기반 테스트 실행
```bash
# 🎯 기본 JMeter 테스트 (Docker)
./gradlew jmeterTest

# 🌊 백만 사용자 JMeter 테스트
./gradlew jmeterMillionTest

# 🚀 K6 극한 테스트
./gradlew k6Test

# 🔥 5백만명 K6 ULTIMATE 테스트
./gradlew k6UltimateTest

# 🚀 모든 테스트 통합 실행
./gradlew ultimateLoadTest
```

### 3단계: 통합 환경 실행 (권장)
```bash
# 대화형 메뉴로 테스트 선택
./scripts/run-extreme-tests.sh

# 또는 Docker Compose 직접 실행
docker-compose -f docker-compose.extreme.yml up -d
```

---

## 📊 모니터링 대시보드

### 실시간 모니터링 접속
```bash
# 🎯 Grafana 대시보드
http://localhost:3000
# ID: admin, PW: extreme_admin

# 📈 Prometheus 메트릭
http://localhost:9091

# 🔔 AlertManager
http://localhost:9093

# 🏥 애플리케이션 헬스체크
http://localhost:8080/actuator/health
```

---

## 🐒 Chaos Engineering

### Chaos Monkey API 엔드포인트
```bash
# 현재 상태 확인
curl http://localhost:8080/api/v1/chaos/status

# 즉시 지연 공격
curl -X POST "http://localhost:8080/api/v1/chaos/attack/latency?maxDelayMs=5000"

# 즉시 예외 공격
curl -X POST "http://localhost:8080/api/v1/chaos/attack/exception"

# 메모리 압박 공격
curl -X POST "http://localhost:8080/api/v1/chaos/attack/memory?sizeMB=100"

# 복합 공격
curl -X POST "http://localhost:8080/api/v1/chaos/attack/combo"

# 극한 모드 활성화
curl -X POST "http://localhost:8080/api/v1/chaos/extreme-mode"

# 모든 공격 중지
curl -X POST "http://localhost:8080/api/v1/chaos/stop"
```

### 특수 시나리오
```bash
# 🛍️ Black Friday 시나리오
curl -X POST "http://localhost:8080/api/v1/chaos/scenarios/blackfriday"

# 💾 Database Outage 시나리오
curl -X POST "http://localhost:8080/api/v1/chaos/scenarios/database-outage"

# 💳 Payment Failure 시나리오
curl -X POST "http://localhost:8080/api/v1/chaos/scenarios/payment-failure"

# 🌐 Network Partition 시나리오
curl -X POST "http://localhost:8080/api/v1/chaos/scenarios/network-partition"
```

---

## 🔥 극한 성능 API

### 성능 테스트 엔드포인트
```bash
# 🌊 5백만명 동시 접속 시뮬레이션
curl -X POST "http://localhost:8080/api/v1/extreme/five-million-users?enableChaos=true"

# 💳 1만명 동시 결제
curl -X POST "http://localhost:8080/api/v1/extreme/ten-thousand-payments"

# ⏰ 24시간 지속 테스트 상태
curl "http://localhost:8080/api/v1/extreme/endurance-test-status"

# 🧠 메모리 누수 탐지
curl -X POST "http://localhost:8080/api/v1/extreme/memory-leak-detection"

# 🌐 네트워크 지연 시뮬레이션
curl -X POST "http://localhost:8080/api/v1/extreme/network-delay-simulation?delayMs=200"

# 💻 CPU 집약적 작업
curl -X POST "http://localhost:8080/api/v1/extreme/cpu-intensive-simulation?iterations=5000000"

# 📊 극한 성능 종합 리포트
curl "http://localhost:8080/api/v1/extreme/performance-report"
```

---

## 🎯 테스트 시나리오 예제

### 시나리오 1: 블랙프라이데이 시뮬레이션
```bash
# 1. 기본 환경 시작
docker-compose -f docker-compose.extreme.yml up -d

# 2. Chaos Monkey 활성화
curl -X POST "http://localhost:8080/api/v1/chaos/extreme-mode"

# 3. 블랙프라이데이 시나리오
curl -X POST "http://localhost:8080/api/v1/chaos/scenarios/blackfriday"

# 4. 동시에 K6 부하 테스트
docker-compose -f docker-compose.extreme.yml --profile k6 up k6-extreme
```

### 시나리오 2: 24시간 내구성 테스트
```bash
# 1. 지속 테스트 시작
./scripts/run-extreme-tests.sh
# 메뉴에서 3번 선택

# 2. 모니터링 (24시간 동안)
# - Grafana: http://localhost:3000
# - 알람: Slack/Discord 웹훅 설정 가능
```

### 시나리오 3: 극한 복구 테스트
```bash
# 1. 정상 부하에서 시작
curl -X POST "http://localhost:8080/api/v1/extreme/five-million-users"

# 2. 의도적 장애 주입
curl -X POST "http://localhost:8080/api/v1/chaos/attack/combo"

# 3. 시스템 복구 확인
curl "http://localhost:8080/api/v1/extreme/performance-report"
```

---

## 📈 결과 분석

### 테스트 결과 위치
```
build/reports/
├── jmeter/
│   ├── html/index.html          # JMeter HTML 보고서
│   └── results.jtl              # 원시 데이터
├── k6/
│   ├── extreme-results.json     # K6 JSON 결과
│   └── ultimate-results.json    # 5백만명 테스트 결과
└── stress/
    └── index.html               # Spring Boot 스트레스 테스트
```

### 성공 기준
- **응답 시간**: P95 < 1초, P99 < 3초
- **오류율**: < 5%
- **처리량**: > 50,000 req/s
- **메모리**: < 90% 사용률
- **복구 시간**: < 30초

---

## 🚨 장애 복구 가이드

### 일반적인 문제 해결

#### 1. 메모리 부족
```bash
# JVM 힙 메모리 증가
export JAVA_OPTS="-Xms4g -Xmx16g"

# Docker 메모리 제한 증가
docker-compose -f docker-compose.extreme.yml up -d --scale popcorn-backend=2
```

#### 2. 포트 충돌
```bash
# 사용 중인 포트 확인
lsof -i :8080
lsof -i :3000
lsof -i :9091

# 프로세스 종료
kill -9 <PID>
```

#### 3. Docker 리소스 정리
```bash
# 모든 컨테이너 정지
docker-compose -f docker-compose.extreme.yml down

# 시스템 정리
docker system prune -a
```

---

## 🏆 극한 테스트 레벨

### 🥉 BRONZE (입문)
- 1,000명 동시 접속
- 10분 지속
- 기본 메트릭 수집

### 🥈 SILVER (중급)
- 10,000명 동시 접속
- 1시간 지속
- Chaos Monkey 활성화

### 🥇 GOLD (고급)
- 100,000명 동시 접속
- 6시간 지속
- 복합 장애 시나리오

### 🏆 PLATINUM (극한)
- 1,000,000명 동시 접속
- 24시간 지속
- 모든 Chaos 시나리오

### 🌊 LEGENDARY (전설)
- 5,000,000명 동시 접속
- 24시간+ 지속
- 완벽한 장애 복구

---

## 📞 지원 및 문의

### 로그 위치
```bash
# 애플리케이션 로그
docker logs popcorn-backend-extreme

# JVM 로그
monitoring/logs/jvm.log

# 테스트 로그
build/reports/
```

### 성능 튜닝 팁
1. **JVM 최적화**: G1GC 사용, 힙 크기 조정
2. **데이터베이스**: 커넥션 풀 크기 최적화
3. **네트워크**: Keep-Alive 활성화
4. **캐싱**: Redis 또는 Caffeine 적용

### 실시간 지원
- **모니터링**: Grafana 대시보드 24/7 확인
- **알림**: 임계값 초과 시 자동 알림
- **로그**: ELK 스택 연동 가능

---

> 🚀 **"극한을 뛰어넘어, 불가능을 가능하게!"**
>
> Popcorn 극한 테스트 환경으로 시스템의 진정한 한계를 발견하고 뛰어넘으세요.