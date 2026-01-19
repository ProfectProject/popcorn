#!/bin/bash

# InfluxDB 초기화 스크립트
# 극한 테스트용 데이터베이스 및 사용자 설정

set -e

echo "🚀 InfluxDB 초기화 시작..."

# InfluxDB가 시작될 때까지 대기
until influx -host localhost -port 8086 -execute "SHOW DATABASES" > /dev/null 2>&1; do
  echo "⏳ InfluxDB 시작 대기 중..."
  sleep 2
done

echo "📊 데이터베이스 생성 중..."

# K6 테스트용 데이터베이스 생성
influx -host localhost -port 8086 -execute "CREATE DATABASE k6"

# Prometheus 메트릭용 데이터베이스 생성
influx -host localhost -port 8086 -execute "CREATE DATABASE prometheus"

# 성능 테스트용 추가 데이터베이스
influx -host localhost -port 8086 -execute "CREATE DATABASE performance"
influx -host localhost -port 8086 -execute "CREATE DATABASE monitoring"

echo "⚙️ 데이터베이스 정책 설정 중..."

# 데이터 보존 정책 설정 (극한 테스트를 위한 7일 보관)
influx -host localhost -port 8086 -execute "CREATE RETENTION POLICY \"extreme_test\" ON \"k6\" DURATION 7d REPLICATION 1 DEFAULT"
influx -host localhost -port 8086 -execute "CREATE RETENTION POLICY \"extreme_test\" ON \"prometheus\" DURATION 7d REPLICATION 1 DEFAULT"

echo "✅ InfluxDB 초기화 완료!"

# 생성된 데이터베이스 확인
echo "📋 생성된 데이터베이스 목록:"
influx -host localhost -port 8086 -execute "SHOW DATABASES"