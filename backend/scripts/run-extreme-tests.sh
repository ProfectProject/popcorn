#!/bin/bash

# 🚀 Popcorn 극한 테스트 실행 스크립트
# K6, JMeter, Chaos Monkey, Grafana 통합 실행

set -e

echo "🌊🔥🚀 POPCORN 극한 테스트 환경 시작! 🚀🔥🌊"
echo "=========================================="

# 컬러 출력 함수
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${CYAN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_header() {
    echo -e "${PURPLE}$1${NC}"
}

# 테스트 유형 선택
show_menu() {
    print_header "🎯 극한 테스트 메뉴"
    echo "1. 🌊 5백만명 동시 접속 테스트 (K6)"
    echo "2. 💳 1만명 동시 결제 테스트 (JMeter)"
    echo "3. ⏰ 24시간 지속 부하 테스트"
    echo "4. 🐒 Chaos Engineering 통합 테스트"
    echo "5. 🚀 ULTIMATE 모든 테스트 동시 실행"
    echo "6. 📊 모니터링 대시보드만 실행"
    echo "7. 🛑 모든 테스트 중지 및 정리"
    echo "0. 종료"
    echo "=========================================="
}

# Docker Compose 상태 확인
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker가 설치되지 않았습니다!"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        print_error "Docker Compose가 설치되지 않았습니다!"
        exit 1
    fi
}

# 시스템 리소스 확인
check_resources() {
    print_status "시스템 리소스 확인 중..."

    # 메모리 확인 (Linux/macOS)
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        TOTAL_MEM=$(free -g | awk '/^Mem:/{print $2}')
        AVAILABLE_MEM=$(free -g | awk '/^Mem:/{print $7}')
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        TOTAL_MEM=$(sysctl -n hw.memsize | awk '{print int($1/1024/1024/1024)}')
        AVAILABLE_MEM=$TOTAL_MEM # macOS는 available 계산이 복잡함
    fi

    print_status "총 메모리: ${TOTAL_MEM}GB"

    if [ "$TOTAL_MEM" -lt 16 ]; then
        print_warning "권장 메모리: 16GB 이상 (현재: ${TOTAL_MEM}GB)"
        print_warning "극한 테스트 시 성능이 제한될 수 있습니다."
        read -p "계속 진행하시겠습니까? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
}

# 기본 환경 시작
start_base_environment() {
    print_status "기본 모니터링 환경 시작 중..."

    docker-compose -f docker-compose.extreme.yml up -d \
        postgres-extreme \
        prometheus \
        grafana \
        influxdb \
        node-exporter \
        cadvisor

    print_status "애플리케이션 시작 중..."
    docker-compose -f docker-compose.extreme.yml up -d popcorn-backend

    # 헬스체크 대기
    print_status "서비스 준비 상태 확인 중..."
    wait_for_service "http://localhost:8080/actuator/health" "Popcorn Backend"
    wait_for_service "http://localhost:3000" "Grafana"
    wait_for_service "http://localhost:9091" "Prometheus"

    print_success "기본 환경 준비 완료!"
}

# 서비스 준비 대기
wait_for_service() {
    local url=$1
    local service_name=$2
    local max_attempts=30
    local attempt=1

    print_status "${service_name} 준비 대기 중..."

    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "$url" > /dev/null 2>&1; then
            print_success "${service_name} 준비 완료!"
            return 0
        fi

        echo -n "."
        sleep 10
        attempt=$((attempt + 1))
    done

    print_error "${service_name} 준비 실패!"
    return 1
}

# K6 5백만명 테스트
run_k6_five_million() {
    print_header "🌊 K6 5백만명 동시 접속 테스트"
    start_base_environment

    print_status "K6 극한 테스트 시작..."
    docker-compose -f docker-compose.extreme.yml --profile k6 up k6-extreme

    print_success "K6 테스트 완료! 결과는 monitoring/reports/k6/ 에서 확인 가능합니다."
}

# JMeter 1만명 결제 테스트
run_jmeter_payment() {
    print_header "💳 JMeter 1만명 동시 결제 테스트"
    start_base_environment

    print_status "JMeter 극한 결제 테스트 시작..."
    docker-compose -f docker-compose.extreme.yml --profile jmeter up jmeter-extreme

    print_success "JMeter 테스트 완료! 결과는 monitoring/reports/jmeter/ 에서 확인 가능합니다."
}

# 24시간 지속 테스트
run_endurance_test() {
    print_header "⏰ 24시간 지속 부하 테스트"
    start_base_environment

    print_warning "24시간 지속 테스트를 시작합니다."
    print_warning "이 테스트는 24시간 동안 계속 실행됩니다!"
    read -p "정말 시작하시겠습니까? (y/N): " -n 1 -r
    echo

    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_status "테스트 취소됨"
        return
    fi

    # K6 24시간 테스트 (백그라운드)
    print_status "24시간 지속 테스트 시작..."
    docker-compose -f docker-compose.extreme.yml --profile k6 up -d k6-extreme

    print_success "24시간 테스트가 백그라운드에서 실행 중입니다."
    print_status "모니터링: http://localhost:3000 (admin/extreme_admin)"
    print_status "중지하려면: ./scripts/run-extreme-tests.sh 에서 7번 선택"
}

# Chaos Engineering 테스트
run_chaos_test() {
    print_header "🐒 Chaos Engineering 통합 테스트"
    start_base_environment

    # Chaos Monkey 활성화
    print_status "Chaos Monkey 활성화 중..."
    curl -X POST "http://localhost:8080/api/v1/chaos/extreme-mode" || true

    # 중간 부하와 함께 Chaos 테스트
    print_status "Chaos + 부하 테스트 시작..."
    docker-compose -f docker-compose.extreme.yml --profile k6 up k6-extreme &

    # 30분 후 Chaos 공격 시작
    sleep 1800
    print_status "Chaos Monkey 공격 시작..."

    # 다양한 Chaos 시나리오 실행
    chaos_scenarios=("latency" "exception" "memory" "blackfriday" "database-outage")

    for scenario in "${chaos_scenarios[@]}"; do
        print_status "Chaos 시나리오: $scenario"
        curl -X POST "http://localhost:8080/api/v1/chaos/attack/$scenario" || true
        sleep 600  # 10분 간격
    done

    print_success "Chaos Engineering 테스트 완료!"
}

# ULTIMATE 모든 테스트
run_ultimate_test() {
    print_header "🚀 ULTIMATE 모든 테스트 동시 실행"
    print_warning "⚠️  경고: 이 테스트는 시스템에 극한 부하를 가합니다!"
    print_warning "⚠️  권장 사양: 32GB RAM, 16 CPU 코어 이상"

    read -p "정말 ULTIMATE 테스트를 실행하시겠습니까? (y/N): " -n 1 -r
    echo

    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_status "테스트 취소됨"
        return
    fi

    print_status "ULTIMATE 극한 테스트 시작..."

    # 모든 서비스 동시 실행
    docker-compose -f docker-compose.extreme.yml up -d
    docker-compose -f docker-compose.extreme.yml --profile k6 --profile jmeter up -d

    # Chaos Monkey 극한 모드 활성화
    sleep 60
    curl -X POST "http://localhost:8080/api/v1/chaos/extreme-mode" || true

    print_success "🔥 ULTIMATE 테스트 실행 중!"
    print_status "모니터링 대시보드:"
    print_status "  - Grafana: http://localhost:3000 (admin/extreme_admin)"
    print_status "  - Prometheus: http://localhost:9091"
    print_status "  - Application: http://localhost:8080"

    print_status "테스트 중지: ./scripts/run-extreme-tests.sh 에서 7번 선택"
}

# 모니터링 대시보드만 실행
run_monitoring_only() {
    print_header "📊 모니터링 대시보드 실행"

    docker-compose -f docker-compose.extreme.yml up -d \
        postgres-extreme \
        popcorn-backend \
        prometheus \
        grafana \
        influxdb \
        node-exporter \
        cadvisor

    wait_for_service "http://localhost:8080/actuator/health" "Popcorn Backend"
    wait_for_service "http://localhost:3000" "Grafana"

    print_success "모니터링 대시보드 준비 완료!"
    print_status "접속 정보:"
    print_status "  - Grafana: http://localhost:3000 (admin/extreme_admin)"
    print_status "  - Prometheus: http://localhost:9091"
    print_status "  - Application: http://localhost:8080"
}

# 모든 테스트 중지 및 정리
cleanup_all() {
    print_header "🛑 모든 테스트 중지 및 정리"

    print_status "모든 컨테이너 중지 중..."
    docker-compose -f docker-compose.extreme.yml --profile k6 --profile jmeter down

    print_status "리소스 정리 중..."
    docker system prune -f

    print_success "정리 완료!"
}

# 메인 실행 로직
main() {
    check_docker
    check_resources

    while true; do
        echo
        show_menu
        read -p "선택하세요 [0-7]: " choice
        echo

        case $choice in
            1) run_k6_five_million ;;
            2) run_jmeter_payment ;;
            3) run_endurance_test ;;
            4) run_chaos_test ;;
            5) run_ultimate_test ;;
            6) run_monitoring_only ;;
            7) cleanup_all ;;
            0)
                print_success "극한 테스트 스크립트를 종료합니다."
                exit 0 ;;
            *)
                print_error "잘못된 선택입니다. 0-7 사이의 숫자를 선택하세요."
                ;;
        esac

        echo
        read -p "계속하려면 Enter를 누르세요..."
    done
}

# 스크립트 실행
main "$@"