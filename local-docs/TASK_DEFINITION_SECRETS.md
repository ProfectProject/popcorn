# Task Definition 시크릿 관리 가이드

## 📋 개요

ECS Task Definition에서 현재 하드코딩되어 있거나 시크릿으로 관리해야 하는 민감한 정보들을 분석하고 개선 방안을 제시합니다.

## 🔍 현재 상태 분석

### ✅ 이미 시크릿으로 관리되는 값들

| 서비스 | Secret 이름 | AWS Secrets Manager ARN | 설명 |
|--------|-------------|-------------------------|------|
| **모든 DB 서비스** | `SPRING_DATASOURCE_PASSWORD` | `${DB_SECRET_ARN}:password::` | PostgreSQL 비밀번호 |
| **payment-service** | `TOSS_PAYMENTS_SECRET_KEY` | `goorm-popcorn-${ENVIRONMENT}/payment/toss-secret` | Toss Payments API 키 |
| **payment-service** | `PAYMENT_ENCRYPTION_KEY` | `goorm-popcorn-${ENVIRONMENT}/payment/encryption-key` | 결제 데이터 암호화 키 |

### ❌ 하드코딩되어 개선이 필요한 값들

#### 1. JWT Secret Keys (Critical 🔴)

| 서비스 | 현재 값 | 보안 위험도 | 개선 필요도 |
|--------|---------|-------------|-------------|
| **user-service** | `goorm-popcorn-jwt-secret-key-for-${ENVIRONMENT}-environment` | 🔴 Critical | ✅ 즉시 |
| **backend-service** | `goorm-popcorn-jwt-secret-key-for-${ENVIRONMENT}-environment` | 🔴 Critical | ✅ 즉시 |

**문제점:**
- 예측 가능한 패턴으로 생성
- 모든 환경에서 동일한 패턴 사용
- Git 히스토리에 노출
- 로테이션 불가능

#### 2. 데이터베이스 연결 정보 (Medium 🟡)

| 서비스 | 현재 노출되는 정보 | 보안 위험도 |
|--------|-------------------|-------------|
| **모든 DB 서비스** | `SPRING_DATASOURCE_USERNAME: "postgres"` | 🟡 Medium |
| **모든 DB 서비스** | `SPRING_DATASOURCE_URL` (호스트, 포트, DB명 포함) | 🟡 Medium |

**문제점:**
- 데이터베이스 사용자명 노출
- 데이터베이스 호스트 정보 노출
- 데이터베이스명 노출

#### 3. Redis 연결 정보 (Low 🟢)

| 서비스 | 현재 노출되는 정보 | 보안 위험도 |
|--------|-------------------|-------------|
| **모든 Redis 서비스** | `REDIS_PORT: "6379"` | 🟢 Low |

**참고:** Redis 호스트는 이미 환경변수 `${REDIS_PRIMARY_ENDPOINT}`로 관리됨

## 🛠️ 개선 방안

### 1. JWT Secret 개선 (최우선)

#### 현재 상태
```json
{
  "name": "JWT_SECRET_KEY",
  "value": "goorm-popcorn-jwt-secret-key-for-${ENVIRONMENT}-environment"
}
```

#### 개선 후
```json
{
  "name": "JWT_SECRET_KEY",
  "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:${AWS_ACCOUNT_ID}:secret:goorm-popcorn-${ENVIRONMENT}/jwt-secret:secret_key::"
}
```

#### AWS Secrets Manager 설정
```bash
# 개발 환경
aws secretsmanager create-secret \
  --name "goorm-popcorn-dev/jwt-secret" \
  --description "JWT signing secret for development" \
  --secret-string '{"secret_key":"randomly-generated-256-bit-key"}'

# 프로덕션 환경
aws secretsmanager create-secret \
  --name "goorm-popcorn-prod/jwt-secret" \
  --description "JWT signing secret for production" \
  --secret-string '{"secret_key":"different-randomly-generated-256-bit-key"}'
```

### 2. 데이터베이스 연결 정보 개선

#### 현재 상태
```json
{
  "name": "SPRING_DATASOURCE_URL",
  "value": "jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
},
{
  "name": "SPRING_DATASOURCE_USERNAME",
  "value": "postgres"
}
```

#### 개선 후
```json
{
  "name": "SPRING_DATASOURCE_URL",
  "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:${AWS_ACCOUNT_ID}:secret:goorm-popcorn-${ENVIRONMENT}/database/credentials:url::"
},
{
  "name": "SPRING_DATASOURCE_USERNAME",
  "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:${AWS_ACCOUNT_ID}:secret:goorm-popcorn-${ENVIRONMENT}/database/credentials:username::"
}
```

#### AWS Secrets Manager 설정
```bash
aws secretsmanager update-secret \
  --secret-id "goorm-popcorn-dev/database/credentials" \
  --secret-string '{
    "password": "current-password",
    "username": "postgres",
    "host": "dev-db-host.amazonaws.com",
    "port": "5432",
    "dbname": "goorm_popcorn_dev",
    "url": "jdbc:postgresql://dev-db-host.amazonaws.com:5432/goorm_popcorn_dev"
  }'
```

### 3. Redis 인증 정보 추가 (권장)

#### 현재 상태 (인증 없음)
```json
{
  "name": "REDIS_HOST",
  "value": "${REDIS_PRIMARY_ENDPOINT}"
},
{
  "name": "REDIS_PORT",
  "value": "6379"
}
```

#### 개선 후 (AUTH 토큰 추가)
```json
{
  "name": "REDIS_HOST",
  "value": "${REDIS_PRIMARY_ENDPOINT}"
},
{
  "name": "REDIS_PORT",
  "value": "6379"
},
{
  "name": "REDIS_AUTH_TOKEN",
  "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:${AWS_ACCOUNT_ID}:secret:goorm-popcorn-${ENVIRONMENT}/redis/auth:token::"
}
```

### 4. 서비스 간 통신 인증 추가 (권장)

#### 현재 상태 (인증 없음)
```json
{
  "name": "USER_SERVICE_URL",
  "value": "http://user-service.goormpopcorn.local:8080"
}
```

#### 개선 후 (API 키 추가)
```json
{
  "name": "USER_SERVICE_URL",
  "value": "http://user-service.goormpopcorn.local:8080"
},
{
  "name": "USER_SERVICE_API_KEY",
  "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:${AWS_ACCOUNT_ID}:secret:goorm-popcorn-${ENVIRONMENT}/internal-api/keys:user_service::"
}
```

## 📊 서비스별 시크릿 요구사항

### Backend Service
```yaml
현재 시크릿:
  ✅ SPRING_DATASOURCE_PASSWORD

추가 필요 시크릿:
  🔴 JWT_SECRET_KEY (Critical)
  🟡 SPRING_DATASOURCE_USERNAME (Medium)
  🟡 SPRING_DATASOURCE_URL (Medium)
```

### User Service
```yaml
현재 시크릿:
  ✅ SPRING_DATASOURCE_PASSWORD

추가 필요 시크릿:
  🔴 JWT_SECRET_KEY (Critical)
  🟡 SPRING_DATASOURCE_USERNAME (Medium)
  🟡 SPRING_DATASOURCE_URL (Medium)
```

### Payment Service
```yaml
현재 시크릿:
  ✅ SPRING_DATASOURCE_PASSWORD
  ✅ TOSS_PAYMENTS_SECRET_KEY
  ✅ PAYMENT_ENCRYPTION_KEY

추가 필요 시크릿:
  🟡 SPRING_DATASOURCE_USERNAME (Medium)
  🟡 SPRING_DATASOURCE_URL (Medium)
```

### Order Service
```yaml
현재 시크릿:
  ✅ SPRING_DATASOURCE_PASSWORD

추가 필요 시크릿:
  🟡 SPRING_DATASOURCE_USERNAME (Medium)
  🟡 SPRING_DATASOURCE_URL (Medium)
  🟢 서비스 간 통신 API 키 (권장)
```

### API Gateway
```yaml
현재 시크릿:
  없음

추가 필요 시크릿:
  🟢 서비스 간 통신 API 키 (권장)
  🟢 REDIS_AUTH_TOKEN (권장)
```

### QR Service
```yaml
현재 시크릿:
  없음

추가 필요 시크릿:
  🟢 서비스 간 통신 API 키 (권장)
  🟢 REDIS_AUTH_TOKEN (권장)
  🟢 S3 접근 키 (현재 IAM Role 사용)
```

## 🔧 개선된 Task Definition 예시

### User Service (개선 후)
```json
{
  "environment": [
    {
      "name": "SPRING_PROFILES_ACTIVE",
      "value": "${ENVIRONMENT}"
    },
    {
      "name": "SERVER_PORT",
      "value": "8080"
    },
    {
      "name": "REDIS_PORT",
      "value": "6379"
    },
    {
      "name": "JWT_EXPIRATION_TIME",
      "value": "86400000"
    }
  ],
  "secrets": [
    {
      "name": "SPRING_DATASOURCE_URL",
      "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:${AWS_ACCOUNT_ID}:secret:goorm-popcorn-${ENVIRONMENT}/database/credentials:url::"
    },
    {
      "name": "SPRING_DATASOURCE_USERNAME",
      "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:${AWS_ACCOUNT_ID}:secret:goorm-popcorn-${ENVIRONMENT}/database/credentials:username::"
    },
    {
      "name": "SPRING_DATASOURCE_PASSWORD",
      "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:${AWS_ACCOUNT_ID}:secret:goorm-popcorn-${ENVIRONMENT}/database/credentials:password::"
    },
    {
      "name": "JWT_SECRET_KEY",
      "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:${AWS_ACCOUNT_ID}:secret:goorm-popcorn-${ENVIRONMENT}/jwt-secret:secret_key::"
    },
    {
      "name": "REDIS_AUTH_TOKEN",
      "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:${AWS_ACCOUNT_ID}:secret:goorm-popcorn-${ENVIRONMENT}/redis/auth:token::"
    }
  ]
}
```

## 🚀 마이그레이션 계획

### Phase 1: Critical 보안 이슈 해결 (즉시)
```yaml
우선순위 1:
  - JWT Secret Key를 AWS Secrets Manager로 이전
  - 모든 환경에서 다른 키 사용
  - 기존 하드코딩된 값 제거

예상 소요 시간: 1-2일
영향도: 높음 (재배포 필요)
```

### Phase 2: 데이터베이스 연결 정보 보안 강화 (1주일 내)
```yaml
우선순위 2:
  - 데이터베이스 연결 정보를 Secrets Manager로 이전
  - 사용자명, URL 정보 보호
  - 연결 문자열 통합 관리

예상 소요 시간: 2-3일
영향도: 중간 (재배포 필요)
```

### Phase 3: 추가 보안 강화 (2주일 내)
```yaml
우선순위 3:
  - Redis 인증 토큰 추가
  - 서비스 간 통신 API 키 도입
  - 모니터링 및 로테이션 설정

예상 소요 시간: 1주일
영향도: 낮음 (점진적 적용 가능)
```

## 📋 마이그레이션 체크리스트

### Phase 1: JWT Secret 마이그레이션
- [ ] AWS Secrets Manager에 JWT secret 생성
- [ ] 개발 환경 Task Definition 업데이트
- [ ] 개발 환경 배포 및 테스트
- [ ] 프로덕션 환경 Task Definition 업데이트
- [ ] 프로덕션 환경 배포
- [ ] 기존 하드코딩된 값 제거 확인

### Phase 2: 데이터베이스 정보 마이그레이션
- [ ] 기존 DB secret에 추가 정보 업데이트
- [ ] Task Definition에서 secrets 섹션으로 이전
- [ ] 각 서비스별 배포 및 테스트
- [ ] 연결 정상 동작 확인

### Phase 3: 추가 보안 강화
- [ ] Redis AUTH 토큰 설정
- [ ] 서비스 간 API 키 생성
- [ ] 점진적 배포 및 테스트
- [ ] 모니터링 설정

## 🔍 검증 방법

### 1. Secret 접근 테스트
```bash
# ECS 컨테이너 내에서 환경변수 확인
aws ecs execute-command \
  --cluster goorm-popcorn-dev-cluster \
  --task <task-id> \
  --container user-service \
  --interactive \
  --command "/bin/bash"

# 컨테이너 내에서
echo $JWT_SECRET_KEY  # 값이 올바르게 로드되는지 확인
```

### 2. 애플리케이션 로그 확인
```bash
# CloudWatch 로그에서 secret 관련 오류 확인
aws logs filter-log-events \
  --log-group-name "/aws/ecs/goorm-popcorn-dev/user-service" \
  --filter-pattern "ERROR"
```

### 3. 보안 스캔 재실행
```bash
# 개선 후 보안 스캔으로 하드코딩된 값 제거 확인
gh workflow run secret-scan.yml
```

## 💰 비용 영향

### AWS Secrets Manager 비용
```yaml
예상 비용 (월간):
  - Secret 저장: $0.40 per secret per month
  - API 호출: $0.05 per 10,000 requests

예상 총 비용:
  - 개발 환경: ~$5/월 (10개 secret)
  - 프로덕션 환경: ~$5/월 (10개 secret)
  - 총 비용: ~$10/월

ROI:
  - 보안 사고 방지로 인한 비용 절약
  - 컴플라이언스 요구사항 충족
  - 자동화된 로테이션으로 운영 효율성 증대
```

## 🔗 관련 문서

- [AWS Secrets Manager 가이드](https://docs.aws.amazon.com/secretsmanager/)
- [ECS Task Definition Secrets](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/specifying-sensitive-data-secrets.html)
- [GitHub Secrets Management](./SECRETS_MANAGEMENT.md)
- [보안 스캔 가이드](./secret-scan.yml)

---

**문서 버전**: 1.0  
**최종 업데이트**: 2024-01-26  
**작성자**: DevOps Team  
**검토자**: Security Team