# GitHub Secrets 관리 가이드

## 📋 개요

Goorm Popcorn 프로젝트에서 사용하는 모든 민감한 정보는 GitHub Secrets와 AWS Secrets Manager를 통해 안전하게 관리됩니다.

## 🔐 GitHub Repository Secrets

### 필수 Secrets (모든 워크플로우 공통)

| Secret 이름 | 설명 | 사용 위치 | 예시 값 |
|-------------|------|-----------|---------|
| `AWS_ROLE_ARN` | AWS IAM 역할 ARN | 모든 배포 워크플로우 | `arn:aws:iam::375896310755:role/github-actions-role` |
| `DISCORD_WEBHOOK` | Discord 웹훅 URL | 모든 알림 | `https://discord.com/api/webhooks/...` |
| `GITHUB_TOKEN` | GitHub API 토큰 | 릴리스 태그 생성 | 자동 제공 |

### 보안 스캔 관련 Secrets

| Secret 이름 | 설명 | 사용 위치 | 필수 여부 |
|-------------|------|-----------|-----------|
| `SNYK_TOKEN` | Snyk 보안 스캔 토큰 | 모든 보안 스캔 | ✅ 필수 |
| `SONAR_TOKEN` | SonarQube 토큰 | SAST 정적 분석 | ✅ 필수 |
| `SONAR_HOST_URL` | SonarQube 서버 URL | SAST 정적 분석 | ✅ 필수 |

### 승인 프로세스 관련 Secrets

| Secret 이름 | 설명 | 사용 위치 | 예시 값 |
|-------------|------|-----------|---------|
| `PROD_APPROVERS` | 프로덕션 승인자 목록 | 수동 승인 프로세스 | `tech-lead-team,security-team` |

### 코드 품질 관련 Secrets (선택사항)

| Secret 이름 | 설명 | 사용 위치 | 필수 여부 |
|-------------|------|-----------|-----------|
| `CODECOV_TOKEN` | Codecov 토큰 | 코드 커버리지 업로드 | 🔶 선택 |

## 🏗️ AWS Secrets Manager

### 데이터베이스 관련 Secrets

```yaml
Secret ARN: ${DB_SECRET_ARN}
Secret 내용:
  - password: PostgreSQL 데이터베이스 비밀번호
  - username: PostgreSQL 사용자명 (선택사항)
  - host: 데이터베이스 호스트 (선택사항)
  - port: 데이터베이스 포트 (선택사항)

사용 서비스: 모든 서비스
Task Definition 참조:
  - name: SPRING_DATASOURCE_PASSWORD
  - valueFrom: ${DB_SECRET_ARN}:password::
```

### Payment Service 전용 Secrets

#### 1. Toss Payments API Key
```yaml
Secret ARN: arn:aws:secretsmanager:ap-northeast-2:${AWS_ACCOUNT_ID}:secret:goorm-popcorn-${ENVIRONMENT}/payment/toss-secret
Secret 내용:
  - secret_key: Toss Payments Secret Key
  - client_key: Toss Payments Client Key (선택사항)

사용 서비스: payment-service
Task Definition 참조:
  - name: TOSS_PAYMENTS_SECRET_KEY
  - valueFrom: arn:aws:secretsmanager:ap-northeast-2:${AWS_ACCOUNT_ID}:secret:goorm-popcorn-${ENVIRONMENT}/payment/toss-secret
```

#### 2. Payment Encryption Key
```yaml
Secret ARN: arn:aws:secretsmanager:ap-northeast-2:${AWS_ACCOUNT_ID}:secret:goorm-popcorn-${ENVIRONMENT}/payment/encryption-key
Secret 내용:
  - encryption_key: AES 암호화 키 (256-bit)
  - salt: 암호화 솔트 (선택사항)

사용 서비스: payment-service
Task Definition 참조:
  - name: PAYMENT_ENCRYPTION_KEY
  - valueFrom: arn:aws:secretsmanager:ap-northeast-2:${AWS_ACCOUNT_ID}:secret:goorm-popcorn-${ENVIRONMENT}/payment/encryption-key
```

### JWT 관련 Secrets (권장)

```yaml
Secret ARN: arn:aws:secretsmanager:ap-northeast-2:${AWS_ACCOUNT_ID}:secret:goorm-popcorn-${ENVIRONMENT}/jwt-secret
Secret 내용:
  - secret_key: JWT 서명용 비밀키
  - refresh_secret: Refresh Token용 비밀키 (선택사항)

사용 서비스: user-service, backend-service
현재 상태: 환경변수로 하드코딩됨 (보안 개선 필요)
```

## 🔧 Secrets 설정 방법

### 1. GitHub Repository Secrets 설정

#### 웹 UI를 통한 설정
```bash
1. GitHub 저장소 → Settings
2. 좌측 메뉴 → Secrets and variables → Actions
3. New repository secret 클릭
4. Name과 Secret 입력 후 Add secret
```

#### GitHub CLI를 통한 설정
```bash
# 기본 설정
gh secret set AWS_ROLE_ARN --body "arn:aws:iam::375896310755:role/github-actions-role"
gh secret set DISCORD_WEBHOOK --body "https://discord.com/api/webhooks/..."
gh secret set SNYK_TOKEN --body "your-snyk-token"
gh secret set SONAR_TOKEN --body "your-sonar-token"
gh secret set SONAR_HOST_URL --body "https://sonarcloud.io"
gh secret set PROD_APPROVERS --body "tech-lead-team,security-team"

# 선택사항
gh secret set CODECOV_TOKEN --body "your-codecov-token"
```

### 2. AWS Secrets Manager 설정

#### AWS CLI를 통한 설정
```bash
# 데이터베이스 비밀번호
aws secretsmanager create-secret \
  --name "goorm-popcorn-dev/database/credentials" \
  --description "PostgreSQL database credentials" \
  --secret-string '{"password":"your-db-password","username":"postgres"}'

# Toss Payments Secret
aws secretsmanager create-secret \
  --name "goorm-popcorn-dev/payment/toss-secret" \
  --description "Toss Payments API credentials" \
  --secret-string '{"secret_key":"test_sk_..."}'

# Payment Encryption Key
aws secretsmanager create-secret \
  --name "goorm-popcorn-dev/payment/encryption-key" \
  --description "Payment service encryption key" \
  --secret-string '{"encryption_key":"your-256-bit-key"}'

# JWT Secret (권장)
aws secretsmanager create-secret \
  --name "goorm-popcorn-dev/jwt-secret" \
  --description "JWT signing secret" \
  --secret-string '{"secret_key":"your-jwt-secret-key"}'
```

#### Terraform을 통한 설정 (권장)
```hcl
# secrets.tf
resource "aws_secretsmanager_secret" "db_credentials" {
  name        = "goorm-popcorn-${var.environment}/database/credentials"
  description = "PostgreSQL database credentials"
  
  tags = {
    Environment = var.environment
    Project     = "goorm-popcorn"
  }
}

resource "aws_secretsmanager_secret_version" "db_credentials" {
  secret_id = aws_secretsmanager_secret.db_credentials.id
  secret_string = jsonencode({
    password = var.db_password
    username = "postgres"
  })
}

resource "aws_secretsmanager_secret" "toss_payments" {
  name        = "goorm-popcorn-${var.environment}/payment/toss-secret"
  description = "Toss Payments API credentials"
  
  tags = {
    Environment = var.environment
    Project     = "goorm-popcorn"
    Service     = "payment"
  }
}

resource "aws_secretsmanager_secret_version" "toss_payments" {
  secret_id = aws_secretsmanager_secret.toss_payments.id
  secret_string = jsonencode({
    secret_key = var.toss_payments_secret_key
  })
}
```

## 🔍 Secrets 검증 방법

### 1. GitHub Secrets 확인
```bash
# GitHub CLI로 설정된 secrets 목록 확인
gh secret list

# 특정 secret 존재 여부 확인 (값은 보이지 않음)
gh secret list | grep AWS_ROLE_ARN
```

### 2. AWS Secrets Manager 확인
```bash
# Secret 목록 확인
aws secretsmanager list-secrets --query 'SecretList[?contains(Name, `goorm-popcorn`)].Name'

# 특정 Secret 정보 확인
aws secretsmanager describe-secret --secret-id "goorm-popcorn-dev/database/credentials"

# Secret 값 확인 (주의: 민감한 정보)
aws secretsmanager get-secret-value --secret-id "goorm-popcorn-dev/database/credentials" --query 'SecretString' --output text
```

### 3. 워크플로우에서 Secrets 테스트
```yaml
# 테스트용 워크플로우 단계
- name: Test Secrets
  run: |
    echo "Testing secrets availability..."
    if [ -z "${{ secrets.AWS_ROLE_ARN }}" ]; then
      echo "❌ AWS_ROLE_ARN is not set"
      exit 1
    else
      echo "✅ AWS_ROLE_ARN is set"
    fi
    
    if [ -z "${{ secrets.DISCORD_WEBHOOK }}" ]; then
      echo "❌ DISCORD_WEBHOOK is not set"
      exit 1
    else
      echo "✅ DISCORD_WEBHOOK is set"
    fi
```

## 🛡️ 보안 모범 사례

### 1. Secrets 관리 원칙
```yaml
✅ DO (해야 할 것):
  - 모든 민감한 정보는 Secrets로 관리
  - 환경별로 다른 Secret 사용 (dev/prod)
  - 정기적인 Secret 로테이션
  - 최소 권한 원칙 적용
  - Secret 접근 로그 모니터링

❌ DON'T (하지 말 것):
  - 코드에 하드코딩
  - 로그에 Secret 값 출력
  - 불필요한 Secret 공유
  - 만료된 Secret 방치
  - 테스트 환경에 프로덕션 Secret 사용
```

### 2. Secret 네이밍 컨벤션
```yaml
GitHub Secrets:
  - 대문자와 언더스코어 사용
  - 명확하고 설명적인 이름
  - 예: AWS_ROLE_ARN, DISCORD_WEBHOOK, SNYK_TOKEN

AWS Secrets Manager:
  - 계층적 구조 사용
  - 환경/서비스/용도 구분
  - 예: goorm-popcorn-dev/payment/toss-secret
```

### 3. Secret 로테이션 전략
```yaml
로테이션 주기:
  - 데이터베이스 비밀번호: 90일
  - API 키: 180일
  - JWT Secret: 30일
  - 암호화 키: 1년

로테이션 방법:
  1. 새 Secret 생성
  2. 애플리케이션에서 새 Secret 사용 확인
  3. 이전 Secret 비활성화
  4. 일정 기간 후 이전 Secret 삭제
```

## 🚨 보안 개선 권장사항

### 1. 현재 하드코딩된 값들을 Secret으로 이전

#### JWT Secret 개선
```yaml
현재 상태:
  - Task Definition에 하드코딩
  - 환경변수: JWT_SECRET_KEY="goorm-popcorn-jwt-secret-key-for-${ENVIRONMENT}-environment"

개선 방안:
  - AWS Secrets Manager로 이전
  - 환경별 다른 키 사용
  - 정기적 로테이션 구현
```

#### 데이터베이스 연결 정보 개선
```yaml
현재 상태:
  - 일부 정보가 환경변수로 노출
  - SPRING_DATASOURCE_URL에 호스트 정보 포함

개선 방안:
  - 모든 DB 연결 정보를 Secrets Manager로 이전
  - 연결 문자열 전체를 Secret으로 관리
```

### 2. 추가 보안 강화

#### Secret 스캔 도구 추가
```yaml
# .github/workflows/secret-scan.yml
name: Secret Scan

on:
  push:
  pull_request:

jobs:
  secret-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run TruffleHog
        uses: trufflesecurity/trufflehog@main
        with:
          path: ./
          base: main
          head: HEAD
```

#### Secret 접근 모니터링
```yaml
CloudWatch 알람:
  - Secret 접근 빈도 모니터링
  - 비정상적 접근 패턴 감지
  - 실패한 Secret 접근 알림
```

## 📊 Secrets 체크리스트

### 설정 완료 체크리스트

#### GitHub Repository Secrets
- [ ] `AWS_ROLE_ARN` - AWS 배포 권한
- [ ] `DISCORD_WEBHOOK` - Discord 알림
- [ ] `SNYK_TOKEN` - 보안 스캔
- [ ] `SONAR_TOKEN` - 정적 분석
- [ ] `SONAR_HOST_URL` - SonarQube 서버
- [ ] `PROD_APPROVERS` - 승인자 목록
- [ ] `CODECOV_TOKEN` - 코드 커버리지 (선택)

#### AWS Secrets Manager
- [ ] Database credentials (`${DB_SECRET_ARN}`)
- [ ] Toss Payments secret (payment-service)
- [ ] Payment encryption key (payment-service)
- [ ] JWT secret (권장 개선사항)

### 보안 검증 체크리스트
- [ ] 모든 Secret이 올바르게 설정됨
- [ ] 워크플로우에서 Secret 접근 가능
- [ ] 로그에 Secret 값이 노출되지 않음
- [ ] 환경별로 다른 Secret 사용
- [ ] Secret 로테이션 계획 수립

## 🔗 관련 문서

- [GitHub Secrets 공식 문서](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [AWS Secrets Manager 가이드](https://docs.aws.amazon.com/secretsmanager/)
- [Discord 웹훅 설정 가이드](./DISCORD_SETUP.md)
- [보안 스캔 도구 설정](./SECURITY_SCAN_SETUP.md)

---

**문서 버전**: 1.0  
**최종 업데이트**: 2024-01-26  
**작성자**: DevOps Team  
**검토자**: Security Team