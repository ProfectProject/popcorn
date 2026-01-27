# GitHub Actions CI/CD 워크플로우

이 디렉토리는 Goorm Popcorn 마이크로서비스의 CI/CD 파이프라인을 포함합니다.

## 📁 워크플로우 구조

```
.github/workflows/
├── api-gateway.yml          # API Gateway 서비스 CI/CD
├── user-service.yml         # User Service CI/CD
├── store-service.yml        # Store Service CI/CD
├── order-service.yml        # Order Service CI/CD
├── payment-service.yml      # Payment Service CI/CD (강화된 보안)
├── qr-service.yml          # QR Service CI/CD
├── order-query.yml         # Order Query Service CI/CD
├── multi-service-deploy.yml # 다중 서비스 배포
└── README.md               # 이 문서
```

## 🚀 배포 전략

### 자동 배포 트리거

**개별 서비스 배포**:
- **트리거**: 해당 서비스 디렉토리 또는 Task Definition 파일 변경
- **브랜치**: `develop` (스테이징), `main` (프로덕션)
- **예시**: `users/` 디렉토리 변경 시 `user-service.yml` 워크플로우 실행

**다중 서비스 배포**:
- **트리거**: 여러 서비스 동시 변경 또는 수동 실행
- **의존성 기반 순차 배포**: 서비스 간 의존성을 고려한 그룹별 배포

### 배포 그룹 및 순서

```yaml
Group 1 (병렬 배포):
  - user-service    # 독립적 인증 서비스
  - qr-service      # 독립적 QR 생성 서비스

Group 2 (병렬 배포):
  - store-service   # user-service 의존
  - order-service   # user-service, store-service 의존
  - order-query     # 읽기 전용 서비스

Group 3 (순차 배포):
  - payment-service # 결제 서비스 (높은 안정성 요구)
  - api-gateway     # 모든 서비스 라우팅 (마지막 배포)
```

## 🔧 환경별 배포 방식

### 스테이징 환경 (`develop` 브랜치)
- **배포 방식**: Rolling Update (ECS 기본)
- **승인**: 자동 배포
- **목적**: QA 테스트 및 통합 검증
- **리소스**: 비용 효율적 설정

### 프로덕션 환경 (`main` 브랜치)
- **배포 방식**: Blue/Green (AWS CodeDeploy)
- **승인**: 수동 승인 필요
- **목적**: 무중단 서비스 제공
- **리소스**: 고가용성 설정

## 📋 필수 GitHub Secrets

워크플로우 실행을 위해 다음 Secrets을 설정해야 합니다:

### AWS 관련
```yaml
AWS_ROLE_ARN: arn:aws:iam::375896310755:role/github-actions-role
```

### 보안 스캔
```yaml
SNYK_TOKEN: your-snyk-token
```

### 알림
```yaml
DISCORD_WEBHOOK: https://discord.com/api/webhooks/...
```

### 승인자
```yaml
PROD_APPROVERS: tech-lead-team,security-team
```

## 🔍 워크플로우 상세 설명

### 1. 개별 서비스 워크플로우

각 서비스별 워크플로우는 다음 단계를 포함합니다:

#### Pull Request 단계
```yaml
1. 코드 체크아웃
2. 환경 설정 (JDK 17, Gradle)
3. 테스트 실행
   - 단위 테스트
   - 통합 테스트 (PostgreSQL, Redis, Kafka)
4. 코드 커버리지 측정
5. 보안 스캔 (Snyk, Trivy)
6. 테스트 리포트 생성
```

#### Push 단계 (배포)
```yaml
1. 애플리케이션 빌드
2. Docker 이미지 빌드
3. 보안 스캔 (이미지)
4. ECR 푸시
5. 환경별 배포
   - develop → 스테이징 (자동)
   - main → 프로덕션 (수동 승인)
6. Slack 알림
```

### 2. 다중 서비스 워크플로우

#### 변경 감지
```yaml
- 수정된 서비스 자동 감지
- 의존성 기반 배포 순서 결정
- 매트릭스 전략으로 병렬 배포
```

#### 수동 배포
```yaml
- 배포할 서비스 선택 가능
- 환경 선택 (dev/prod)
- 이미지 태그 지정 가능
```

## 🛡️ 보안 강화 (Payment Service)

결제 서비스는 추가 보안 조치를 적용합니다:

```yaml
보안 스캔:
  - 강화된 취약점 스캔 (MEDIUM 이상)
  - SAST (정적 애플리케이션 보안 테스트)
  - 의존성 보안 검사

승인 프로세스:
  - 최소 2명 승인 필요
  - Tech Lead + Security Lead 승인
  - 상세한 보안 체크리스트

배포 후 모니터링:
  - 강화된 모니터링 활성화
  - 실시간 보안 알림
```

## 📊 모니터링 및 알림

### Discord 알림
모든 배포 상태는 Discord 채널로 알림됩니다:

```yaml
알림 내용:
  - 배포 시작/완료/실패
  - 환경 정보 (🟡 Staging/🔴 Production)
  - 배포된 서비스 목록
  - 작성자 정보
  - 버전 정보 (Git SHA)
  - 서비스별 이모지 구분
```

### 테스트 리포트
- **단위 테스트**: JUnit 리포트 자동 생성
- **커버리지**: Codecov 통합
- **보안 스캔**: GitHub Security 탭에서 확인

## 🚀 사용 방법

### 1. 개별 서비스 배포

**자동 배포**:
```bash
# 스테이징 배포
git checkout develop
git add users/src/main/java/...
git commit -m "feat: 사용자 인증 기능 추가"
git push origin develop
# → user-service.yml 워크플로우 자동 실행

# 프로덕션 배포
git checkout main
git merge develop
git push origin main
# → 수동 승인 후 배포
```

### 2. 다중 서비스 배포

**수동 배포**:
1. GitHub Actions 탭 이동
2. "Multi-Service Deployment" 워크플로우 선택
3. "Run workflow" 클릭
4. 옵션 설정:
   - Services: `user-service,order-service` 또는 `all`
   - Environment: `dev` 또는 `prod`
   - Image tag: `latest` 또는 특정 태그

### 3. 긴급 롤백

```bash
# 이전 버전으로 긴급 롤백
cd .aws
./deploy.sh user-service prod previous-working-tag
```

## 🔧 로컬 테스트

워크플로우를 로컬에서 테스트하려면:

```bash
# Act 도구 설치 (GitHub Actions 로컬 실행)
brew install act

# 워크플로우 테스트
act -j test --secret-file .secrets
```

## 📈 성능 최적화

### 빌드 캐시
- **Gradle 캐시**: 의존성 다운로드 시간 단축
- **Docker Layer 캐시**: 이미지 빌드 시간 단축
- **테스트 결과 캐시**: 중복 테스트 방지

### 병렬 실행
- **서비스별 독립 실행**: 변경된 서비스만 빌드
- **그룹별 병렬 배포**: 의존성 고려한 최적 순서
- **매트릭스 전략**: 동일 그룹 내 병렬 처리

## 🐛 문제 해결

### 일반적인 문제

**1. 테스트 실패**
```yaml
원인: 데이터베이스 연결 실패
해결: services 설정에서 health check 확인
```

**2. 보안 스캔 실패**
```yaml
원인: 취약한 의존성 발견
해결: 의존성 업데이트 또는 예외 처리
```

**3. 배포 실패**
```yaml
원인: AWS 권한 부족
해결: IAM 역할 권한 확인
```

### 디버깅 방법

**1. 워크플로우 로그 확인**
- GitHub Actions 탭에서 실행 로그 확인
- 각 단계별 상세 로그 분석

**2. 로컬 재현**
```bash
# 동일한 환경에서 로컬 테스트
docker-compose -f docker-compose.test.yml up
./gradlew test
```

**3. AWS 리소스 확인**
```bash
# ECS 서비스 상태 확인
aws ecs describe-services --cluster goorm-popcorn-dev-cluster --services user-service

# CloudWatch 로그 확인
aws logs describe-log-groups --log-group-name-prefix "/aws/ecs/goorm-popcorn"
```

## 📚 관련 문서

- [CI/CD 아키텍처 설계](../../popcorn-terraform/docs/cicd-architecture.md)
- [ECS Task Definition 관리](../docs/ecs-task-definition-management.md)
- [AWS 인프라 아키텍처](../../popcorn-terraform/docs/aws-infrastructure-architecture.md)
- [배포 스크립트 가이드](../.aws/README.md)

## 🔄 지속적 개선

### 단기 계획 (1-3개월)
- [ ] E2E 테스트 자동화 추가
- [ ] 성능 테스트 통합
- [ ] 배포 메트릭 대시보드 구축

### 중기 계획 (3-6개월)
- [ ] Canary 배포 도입
- [ ] Feature Flag 통합
- [ ] GitOps 전환 (ArgoCD)

### 장기 계획 (6-12개월)
- [ ] EKS 전환 대비
- [ ] 멀티 클라우드 지원
- [ ] AI 기반 배포 최적화

---

**문서 버전**: 1.0  
**최종 업데이트**: 2024-01-26  
**작성자**: DevOps Team  
**검토자**: Tech Lead