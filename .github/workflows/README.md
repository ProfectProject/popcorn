# GitHub Actions 워크플로우 가이드

## 개요
이 프로젝트는 재사용 가능한 워크플로우를 사용하여 모든 마이크로서비스의 CI/CD를 표준화했습니다.

## 워크플로우 구조

### 재사용 가능한 워크플로우
- **`reusable-build-deploy.yml`**: 모든 서비스가 공통으로 사용하는 빌드/배포 워크플로우

### 서비스별 워크플로우
각 서비스는 재사용 가능한 워크플로우를 호출하며, 서비스별 특성을 매개변수로 전달합니다.

```
.github/workflows/
├── reusable-build-deploy.yml    # 재사용 가능한 워크플로우
├── api-gateway.yml              # API Gateway 서비스
├── user-service.yml             # User Service
├── store-service.yml            # Store Service  
├── order-service.yml            # Order Service
├── payment-service.yml          # Payment Service (보안 강화)
├── checkin-service.yml          # CheckIn Service
└── order-query.yml              # Order Query Service
```

## 서비스별 설정

| 서비스 | 디렉토리 | ECR 레포지토리 | Kafka 필요 | 보안 강화 | 승인 수 | 이모지 |
|--------|----------|----------------|------------|-----------|---------|--------|
| API Gateway | gateway | goorm-popcorn-api-gateway | ❌ | ❌ | 1 | 🚪 |
| User Service | users | goorm-popcorn-user | ❌ | ❌ | 1 | 👤 |
| Store Service | stores | goorm-popcorn-store | ❌ | ❌ | 1 | 🏪 |
| Order Service | order | goorm-popcorn-order | ✅ | ❌ | 1 | 📦 |
| Payment Service | payment | goorm-popcorn-payment | ❌ | ✅ | 2 | 🔒 |
| CheckIn Service | checkIns | goorm-popcorn-checkin | ❌ | ❌ | 1 | 📱 |
| Order Query | orderQuery | goorm-popcorn-order-query | ❌ | ❌ | 1 | 📊 |
| Coupon Service | coupon | goorm-popcorn-coupon | ✅ | ❌ | 1 | 🎟️ |

## 이미지 태그 전략

### Production (main 브랜치)
- `{git-sha-8자리}`: a1b2c3d4
- `v{semantic-version}`: v1.2.3 (태그가 semantic version일 때)
- `latest`: 최신 프로덕션 버전

### Development (develop 브랜치)
- `dev-{git-sha-8자리}`: dev-a1b2c3d4
- `dev-latest`: 최신 개발 버전
- `dev-{YYYYMMDD}`: dev-20240120

### Feature/Hotfix 브랜치
- `feature-{sanitized-branch-name}-{git-sha-8자리}`
- `hotfix-{sanitized-branch-name}-{git-sha-8자리}`
- `pr-{pr-number}-{git-sha-8자리}`

## 워크플로우 트리거

### Push 이벤트
각 서비스는 다음 경로 변경 시 트리거됩니다:
- 서비스 디렉토리 (`users/**`, `gateway/**`, 등)
- 신규 서비스 디렉토리 (`coupon/**` 등)
- Task Definition (`.aws/task-definitions/{service}.json`)
- 워크플로우 파일 (`.github/workflows/{service}.yml`)

### Pull Request 이벤트
- `develop`, `main` 브랜치로의 PR에서 테스트 및 보안 스캔 실행
- 서비스 디렉토리와 Task Definition 변경 시에만 트리거

## 보안 기능

### 표준 보안 스캔
- **Snyk**: 의존성 취약점 스캔
- **Trivy**: 컨테이너 이미지 스캔 (HIGH, CRITICAL)
- **코드 커버리지**: Codecov 업로드

### 강화된 보안 스캔 (Payment Service)
- **Enhanced Trivy**: MEDIUM, HIGH, CRITICAL 스캔
- **SARIF 출력**: 보안 스캔 결과 저장
- **추가 SAST**: 정적 분석 도구
- **2단계 승인**: Tech Lead + Security Lead

## 배포 프로세스

### Development 환경
1. `develop` 브랜치 푸시
2. 자동 빌드 및 테스트
3. ECR에 이미지 푸시 (`dev-*` 태그)
4. 자동 배포 to Staging

### Production 환경
1. `main` 브랜치 푸시
2. 자동 빌드 및 테스트
3. ECR에 이미지 푸시 (production 태그)
4. **수동 승인 대기**
5. 승인 후 Production 배포

### 승인 프로세스
- **일반 서비스**: 1명 승인 필요
- **Payment Service**: 2명 승인 필요 (보안 강화)

## Discord 알림

각 배포 완료 시 Discord로 알림이 전송됩니다:

```
🚪✅ api-gateway 배포 성공

환경: 🟡 Staging
버전: dev-a1b2c3d4
태그: dev-a1b2c3d4,dev-latest,dev-20240120
작성자: developer
브랜치: develop
실행 시간: 42번째 실행
```

## 새 서비스 추가 방법

1. **서비스 워크플로우 생성**:
```yaml
name: New Service CI/CD

on:
  push:
    branches: [develop, main]
    paths:
      - 'newservice/**'
      - '.aws/task-definitions/new-service.json'
      - '.github/workflows/new-service.yml'
  pull_request:
    branches: [develop, main]
    paths:
      - 'newservice/**'
      - '.aws/task-definitions/new-service.json'

jobs:
  build-deploy:
    uses: ./.github/workflows/reusable-build-deploy.yml
    with:
      service-name: new-service
      service-directory: newservice
      ecr-repository: goorm-popcorn-newservice
      java-version: '17'
      gradle-build-args: 'bootJar -x test -x javadoc'
      needs-kafka: false
      enhanced-security: false
      min-approvals: 1
      discord-emoji: '🆕'
    secrets:
      AWS_ROLE_ARN: ${{ secrets.AWS_ROLE_ARN }}
      SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
      PROD_APPROVERS: ${{ secrets.PROD_APPROVERS }}
      DISCORD_WEBHOOK: ${{ secrets.DISCORD_WEBHOOK }}
```

2. **Task Definition 생성**: `.aws/task-definitions/new-service.json`

3. **배포 스크립트 업데이트**: `.aws/deploy.sh`에 서비스 추가

## 신규 서비스 온보딩 체크리스트

1. 애플리케이션 코드 반영
   - 서비스 디렉토리 추가 (`<service>/`)
   - `settings.gradle`에 모듈 include 추가
2. 컨테이너 레지스트리 준비
   - ECR 리포지토리 생성 (`goorm-popcorn-<service>`)
3. CI 워크플로우 반영
   - `.github/workflows/unified-cicd.yml`
     - `paths`, 변경 감지, `services=all`, whitelist, ECR 매핑
   - `.github/workflows/feature-branch-ci.yml`
     - `pull_request.paths`, 변경 감지, common-lib 전체 빌드 목록, ECR 매핑
4. 배포 저장소 반영 (`popcorn_deploy`)
   - `helm/charts/<service>` 차트 추가
   - `helm/popcorn-umbrella/Chart.yaml` dependency 추가
   - `values.yaml`, `values-dev.yaml`, `values-prod.yaml` 서비스 블록 추가
5. 시크릿/환경변수 반영
   - ExternalSecret 템플릿 키 추가
   - 서비스별 `env/envFrom` 값과 Secret 키 이름 정합성 확인
6. 라우팅/관측 반영
   - gateway 라우팅 URL/환경변수 반영
   - 모니터링 대시보드/알림 대상 점검
7. 최종 검증
   - 워크플로우 dry-run 또는 feature 브랜치 빌드 성공 확인
   - Helm template/lint 및 ArgoCD Sync 결과 확인

## 트러블슈팅

### 일반적인 문제

1. **워크플로우가 트리거되지 않음**
   - 파일 경로가 `paths` 설정과 일치하는지 확인
   - 브랜치 이름이 올바른지 확인

2. **이미지 빌드 실패**
   - Gradle 빌드 오류 확인
   - Dockerfile 경로 확인

3. **보안 스캔 실패**
   - 취약점 해결 또는 예외 처리
   - Snyk 토큰 확인

4. **배포 실패**
   - AWS 권한 확인
   - Task Definition 문법 확인
   - ECR 레포지토리 존재 확인

### 로그 확인 방법

1. **GitHub Actions 탭**에서 워크플로우 실행 로그 확인
2. **AWS ECS 콘솔**에서 서비스 상태 확인
3. **CloudWatch Logs**에서 애플리케이션 로그 확인

## 모니터링

### 메트릭
- 빌드 성공률
- 배포 빈도
- 평균 빌드 시간
- 보안 스캔 결과

### 알림 채널
- **Discord**: 배포 상태 알림
- **GitHub Issues**: 승인 요청
- **AWS CloudWatch**: 인프라 모니터링

## 보안 고려사항

### Secrets 관리
- AWS 역할 기반 인증 사용
- 민감한 정보는 GitHub Secrets에 저장
- 환경별 시크릿 분리

### 이미지 보안
- 정기적인 베이스 이미지 업데이트
- 취약점 스캔 자동화
- 최소 권한 원칙 적용

### 네트워크 보안
- VPC 내부 통신
- 보안 그룹 최소화
- TLS 암호화 적용

## 성능 최적화

### 빌드 최적화
- Gradle 캐시 활용
- Docker 레이어 캐싱
- 병렬 빌드 실행

### 배포 최적화
- 롤링 업데이트
- 헬스 체크 최적화
- 리소스 할당 조정

## 참고 문서

- [이미지 태그 전략](../aws/IMAGE_TAG_STRATEGY.md)
- [배포 가이드](../aws/README.md)
- [보안 가이드](../local-docs/SECRETS_MANAGEMENT.md)
- [서비스 트리거 가이드](SERVICE_TRIGGER_GUIDE.md)
