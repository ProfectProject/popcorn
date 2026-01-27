# GitHub Actions 워크플로우 사용 가이드

## 📋 개요

Goorm Popcorn 프로젝트는 **하이브리드 CI/CD 전략**을 사용합니다:

1. **개별 서비스 워크플로우**: 일상적 개발용 (빠른 피드백)
2. **통합 워크플로우**: 대규모 배포 및 수동 배포용 (유연성)

## 🚀 워크플로우 종류

### 1. 개별 서비스 워크플로우

각 서비스별로 독립적인 워크플로우를 제공합니다:

```
├── backend-service.yml      # Backend 서비스 (메인 애플리케이션)
├── user-service.yml         # User 서비스 (인증/사용자 관리)
├── store-service.yml        # Store 서비스 (매장 관리)
├── order-service.yml        # Order 서비스 (주문 처리)
├── order-query.yml          # Order Query 서비스 (주문 조회)
├── payment-service.yml      # Payment 서비스 (결제 처리)
├── api-gateway.yml          # API Gateway (라우팅)
├── qr-service.yml          # QR 서비스 (QR 코드)
└── checkin-service.yml     # CheckIn 서비스 (체크인)
```

**트리거 조건:**
- 해당 서비스 디렉토리 변경
- Task Definition 파일 변경
- 워크플로우 파일 자체 변경

### 2. 통합 워크플로우 (unified-cicd.yml)

모든 서비스를 통합 관리하는 워크플로우입니다.

**트리거 조건:**
- 자동: Push/PR 시 변경된 서비스 자동 감지
- 수동: GitHub Actions UI에서 직접 실행

## 📊 언제 어떤 워크플로우를 사용할까?

### 자동 트리거 (권장)

각 서비스는 **자동으로** 해당 서비스 파일이 변경될 때만 실행됩니다:

```yaml
✅ 자동 실행 조건:
  - users/ 디렉토리 변경 → user-service.yml 실행
  - payment/ 디렉토리 변경 → payment-service.yml 실행
  - backend/ 디렉토리 변경 → backend-service.yml 실행
  - Task Definition 변경 → 해당 서비스 워크플로우 실행

❌ 실행되지 않는 경우:
  - README.md, docs/ 등 문서 파일만 변경
  - 다른 서비스 디렉토리 변경
  - 공통 설정 파일만 변경
```

### 개별 서비스 워크플로우 사용 시기

```yaml
✅ 자동으로 실행되는 상황:
  - 단일 서비스 코드 수정
  - 해당 서비스 Task Definition 변경
  - 해당 서비스 워크플로우 파일 수정
  - 일상적인 개발 작업

⏱️ 장점:
  - 빠른 실행 (3-5분)
  - 명확한 로그
  - 리소스 효율적
  - 다른 서비스에 영향 없음
```

### 통합 워크플로우 사용 시기

```yaml
✅ 수동 실행이 필요한 상황:
  - 여러 서비스 동시 배포
  - 의존성 있는 변경사항
  - 특정 버전으로 배포
  - 환경별 선택 배포
  - 커밋 메시지에 [unified] 또는 [multi] 포함

⏱️ 장점:
  - 의존성 고려한 배포
  - 수동 제어 가능
  - 매트릭스 병렬 처리
  - 유연한 배포 옵션
```

## 🔧 사용 방법

### 1. 자동 배포 (개별 서비스) - 권장 방식

**스테이징 배포:**
```bash
# 1. 특정 서비스만 수정
git checkout develop
echo "// 새로운 기능" >> users/src/main/java/UserController.java

# 2. 커밋 & 푸시
git add users/
git commit -m "feat: 사용자 프로필 기능 추가"
git push origin develop

# 3. 자동 실행
# → user-service.yml 워크플로우만 자동으로 실행됩니다
# → 다른 서비스 워크플로우는 실행되지 않습니다
# → 테스트 → 빌드 → 스테이징 배포
```

**여러 서비스 개별 배포:**
```bash
# 1. 여러 서비스 수정
echo "// API 변경" >> users/src/main/java/UserController.java
echo "// API 변경" >> order/src/main/java/OrderController.java

# 2. 커밋 & 푸시
git add users/ order/
git commit -m "feat: API 스펙 변경"
git push origin develop

# 3. 자동 실행
# → user-service.yml과 order-service.yml이 각각 병렬로 실행됩니다
# → 각 서비스는 독립적으로 배포됩니다
```

**통합 배포가 필요한 경우:**
```bash
# 의존성이 있는 변경사항의 경우
git commit -m "feat: 여러 서비스 API 변경 [unified]"
git push origin develop

# → unified-cicd.yml 워크플로우가 실행됩니다
# → 의존성 순서를 고려한 배포가 진행됩니다
```

**프로덕션 배포:**
```bash
# 1. main 브랜치로 머지
git checkout main
git merge develop
git push origin main

# 2. 수동 승인 대기
# → GitHub에서 승인 이슈가 자동 생성됩니다
# → 승인자가 승인하면 프로덕션 배포 진행
```

### 2. 수동 배포 (통합 워크플로우)

**GitHub Actions UI 사용:**

1. **GitHub 저장소 → Actions 탭 이동**
2. **"Unified CI/CD Pipeline" 워크플로우 선택**
3. **"Run workflow" 버튼 클릭**
4. **옵션 설정:**

```yaml
Services: 
  - "user-service,order-service" (특정 서비스)
  - "all" (모든 서비스)

Environment:
  - "dev" (스테이징)
  - "prod" (프로덕션)

Image tag:
  - "latest" (최신 버전)
  - "v1.2.3" (특정 태그)
  - "abc123" (특정 커밋)
```

### 3. 긴급 롤백

```bash
# 방법 1: 이전 버전으로 수동 배포
# GitHub Actions UI에서:
# Services: "payment-service"
# Environment: "prod"  
# Image tag: "previous-working-commit-sha"

# 방법 2: 배포 스크립트 직접 사용
cd .aws
./deploy.sh payment-service prod previous-working-tag
```

## 🛡️ 보안 및 승인 프로세스

### Payment Service (강화된 보안)

```yaml
보안 조치:
  - 2명 승인 필요 (Tech Lead + Security Lead)
  - 강화된 취약점 스캔 (MEDIUM 이상)
  - 추가 보안 체크리스트
  - 상세한 승인 이슈 템플릿

승인 프로세스:
  1. 배포 요청 → 자동 이슈 생성
  2. 보안 체크리스트 검토
  3. 2명 승인 완료
  4. 자동 배포 진행
```

### 일반 서비스

```yaml
보안 조치:
  - 1명 승인 필요
  - 표준 취약점 스캔 (HIGH 이상)
  - 기본 승인 프로세스

승인 프로세스:
  1. 배포 요청 → 자동 이슈 생성
  2. 기본 체크리스트 검토
  3. 1명 승인 완료
  4. 자동 배포 진행
```

## 📈 모니터링 및 알림

### Discord 알림

모든 배포 상태는 Discord 채널로 실시간 알림됩니다:

```yaml
알림 내용:
  ✅ 배포 성공: "✅ user-service 배포 성공 (🟡 Staging)"
  ❌ 배포 실패: "❌ payment-service 배포 실패 (🔴 Production)"
  ⏳ 승인 대기: "⏳ order-service 승인 대기 중 (🔴 Production)"
  
포함 정보:
  - 서비스 이름 (이모지로 구분)
  - 환경 (🟡 Staging / 🔴 Production)
  - 버전 (Git SHA)
  - 작성자
  - 브랜치
  - 실행 번호

서비스별 이모지:
  - 🔒 payment-service (보안 강화)
  - 🚪 api-gateway (게이트웨이)
  - 📱 qr-service (QR 코드)
  - 📊 order-query (조회 서비스)
  - 기타 서비스: ✅/❌
```

### 테스트 리포트

```yaml
자동 생성 리포트:
  - 단위 테스트 결과 (JUnit)
  - 코드 커버리지 (Codecov)
  - 보안 스캔 결과 (Snyk, Trivy)
  - 성능 테스트 결과 (향후 추가)
```

## 🔍 문제 해결

### 일반적인 문제들

#### 1. 테스트 실패

**증상:**
```
❌ Test failed: UserServiceTest.testCreateUser
```

**해결 방법:**
```bash
# 로컬에서 테스트 재현
cd users
../gradlew test

# 특정 테스트만 실행
../gradlew test --tests UserServiceTest.testCreateUser

# 테스트 환경 확인
docker-compose -f docker-compose.test.yml up -d
```

#### 2. 보안 스캔 실패

**증상:**
```
❌ Snyk found 3 high severity vulnerabilities
```

**해결 방법:**
```bash
# 의존성 업데이트
./gradlew dependencyUpdates

# 취약점 상세 확인
snyk test --file=build.gradle

# 예외 처리 (필요시)
# .snyk 파일에 예외 규칙 추가
```

#### 3. 배포 실패

**증상:**
```
❌ ECS service update failed
```

**해결 방법:**
```bash
# ECS 서비스 상태 확인
aws ecs describe-services \
  --cluster goorm-popcorn-dev-cluster \
  --services user-service

# CloudWatch 로그 확인
aws logs describe-log-groups \
  --log-group-name-prefix "/aws/ecs/goorm-popcorn"

# 수동 롤백
./deploy.sh user-service dev previous-working-tag
```

#### 4. 승인 프로세스 문제

**증상:**
```
⏳ Waiting for approval but no issue created
```

**해결 방법:**
```bash
# GitHub Token 권한 확인
# Settings → Developer settings → Personal access tokens

# 승인자 목록 확인
# Repository → Settings → Secrets → PROD_APPROVERS

# 수동 승인 건너뛰기 (긴급시)
# 워크플로우에서 승인 단계 임시 비활성화
```

## 📚 고급 사용법

### 1. 조건부 배포

특정 조건에서만 배포하고 싶은 경우:

```yaml
# 특정 브랜치에서만 배포
if: github.ref == 'refs/heads/release'

# 특정 파일 변경시에만 배포
paths:
  - 'users/src/main/**'
  - '!users/src/test/**'

# 특정 시간에만 배포 (cron)
schedule:
  - cron: '0 2 * * 1-5'  # 평일 오전 2시
```

### 2. 환경별 설정

```yaml
# 환경별 다른 설정 사용
- name: Set environment variables
  run: |
    if [ "${{ github.ref }}" = "refs/heads/main" ]; then
      echo "ENV=prod" >> $GITHUB_ENV
      echo "CPU=1024" >> $GITHUB_ENV
      echo "MEMORY=2048" >> $GITHUB_ENV
    else
      echo "ENV=dev" >> $GITHUB_ENV
      echo "CPU=256" >> $GITHUB_ENV
      echo "MEMORY=512" >> $GITHUB_ENV
    fi
```

### 3. 병렬 배포 최적화

```yaml
# 의존성 기반 그룹 배포
Group 1 (병렬):
  - user-service
  - qr-service

Group 2 (병렬, Group 1 완료 후):
  - store-service
  - order-service

Group 3 (순차, Group 2 완료 후):
  - payment-service
  - api-gateway
```

## 🔄 지속적 개선

### 현재 구현된 기능

```yaml
✅ 완료된 기능:
  - 서비스별 독립 워크플로우
  - 통합 워크플로우
  - 자동 변경 감지
  - 매트릭스 병렬 처리
  - 환경별 배포
  - 보안 스캔 통합
  - Slack 알림
  - 수동 승인 프로세스
```

### 향후 개선 계획

```yaml
🔮 단기 계획 (1-3개월):
  - E2E 테스트 자동화
  - 성능 테스트 통합
  - 배포 메트릭 대시보드
  - Canary 배포 도입

🔮 중기 계획 (3-6개월):
  - Feature Flag 통합
  - GitOps 전환 (ArgoCD)
  - 자동 롤백 기능
  - AI 기반 배포 최적화

🔮 장기 계획 (6-12개월):
  - EKS 전환 대비
  - 멀티 클라우드 지원
  - 서비스 메시 통합
  - 완전 자동화된 배포
```

## 📞 지원 및 문의

### 문제 발생 시 연락처

```yaml
일반 문의:
  - Slack: #devops-support
  - Email: devops@goormpopcorn.com

긴급 상황:
  - Slack: #incident-response
  - 온콜: +82-10-xxxx-xxxx

문서 개선:
  - GitHub Issues
  - Pull Request 환영
```

### 유용한 링크

```yaml
관련 문서:
  - AWS 인프라 아키텍처: ../../popcorn-terraform/docs/
  - ECS Task Definition 관리: ../.aws/README.md
  - 배포 스크립트 가이드: ../.aws/deploy.sh

모니터링:
  - CloudWatch 대시보드: https://console.aws.amazon.com/cloudwatch/
  - ECS 클러스터: https://console.aws.amazon.com/ecs/
  - ECR 저장소: https://console.aws.amazon.com/ecr/
```

---

**문서 버전**: 1.0  
**최종 업데이트**: 2024-01-26  
**작성자**: DevOps Team  
**검토자**: Tech Lead