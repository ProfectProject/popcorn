# 이미지 태그 전략

## 개요
이 문서는 ECR 이미지 태그 전략을 설명합니다. 모든 서비스는 동일한 태그 전략을 따릅니다.

## 브랜치별 태그 전략

### Production (main 브랜치)
**태그 형식:**
- `{git-sha-8자리}`: a1b2c3d4
- `v{semantic-version}`: v1.2.3 (태그가 semantic version 형식일 때)
- `latest`: 최신 프로덕션 버전
- `stable`: 검증된 안정 버전 (수동 태그)

**예시:**
```
375896310755.dkr.ecr.ap-northeast-2.amazonaws.com/goorm-popcorn-checkin:a1b2c3d4
375896310755.dkr.ecr.ap-northeast-2.amazonaws.com/goorm-popcorn-checkin:v1.2.3
375896310755.dkr.ecr.ap-northeast-2.amazonaws.com/goorm-popcorn-checkin:latest
```

### Development (develop 브랜치)
**태그 형식:**
- `dev-{git-sha-8자리}`: dev-a1b2c3d4
- `dev-latest`: 최신 개발 버전
- `dev-{YYYYMMDD}`: dev-20240120

**예시:**
```
375896310755.dkr.ecr.ap-northeast-2.amazonaws.com/goorm-popcorn-checkin:dev-a1b2c3d4
375896310755.dkr.ecr.ap-northeast-2.amazonaws.com/goorm-popcorn-checkin:dev-latest
375896310755.dkr.ecr.ap-northeast-2.amazonaws.com/goorm-popcorn-checkin:dev-20240120
```

### Feature/Hotfix 브랜치
**태그 형식:**
- `feature-{sanitized-branch-name}-{git-sha-8자리}`
- `hotfix-{sanitized-branch-name}-{git-sha-8자리}`
- `pr-{pr-number}-{git-sha-8자리}`

**예시:**
```
375896310755.dkr.ecr.ap-northeast-2.amazonaws.com/goorm-popcorn-checkin:feature-user-auth-a1b2c3d4
375896310755.dkr.ecr.ap-northeast-2.amazonaws.com/goorm-popcorn-checkin:hotfix-login-bug-a1b2c3d4
375896310755.dkr.ecr.ap-northeast-2.amazonaws.com/goorm-popcorn-checkin:pr-123-a1b2c3d4
```

## 태그 생성 로직

### GitHub Actions에서의 구현
```bash
GIT_SHA=$(git rev-parse --short=8 HEAD)

case "$GITHUB_REF" in
  refs/heads/main)
    TAGS="$GIT_SHA,latest"
    if [[ $GITHUB_REF_NAME =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
      TAGS="$TAGS,$GITHUB_REF_NAME"
    fi
    ;;
  refs/heads/develop)
    TAGS="dev-$GIT_SHA,dev-latest,dev-$(date +%Y%m%d)"
    ;;
  refs/heads/feature/*)
    BRANCH_NAME=$(echo $GITHUB_REF_NAME | sed 's/[^a-zA-Z0-9]/-/g' | tr '[:upper:]' '[:lower:]')
    TAGS="feature-$BRANCH_NAME-$GIT_SHA"
    ;;
  refs/heads/hotfix/*)
    BRANCH_NAME=$(echo $GITHUB_REF_NAME | sed 's/[^a-zA-Z0-9]/-/g' | tr '[:upper:]' '[:lower:]')
    TAGS="hotfix-$BRANCH_NAME-$GIT_SHA"
    ;;
  refs/pull/*)
    PR_NUMBER=$(echo $GITHUB_REF | sed 's/refs\/pull\/\([0-9]*\)\/merge/\1/')
    TAGS="pr-$PR_NUMBER-$GIT_SHA"
    ;;
esac
```

## Task Definition에서의 사용

### 환경별 이미지 태그
- **Development**: `dev-{git-sha-8자리}` 또는 `dev-latest`
- **Production**: `{git-sha-8자리}` 또는 `v{semantic-version}`

### Task Definition 템플릿
```json
{
  "containerDefinitions": [
    {
      "name": "service-name",
      "image": "${AWS_ACCOUNT_ID}.dkr.ecr.ap-northeast-2.amazonaws.com/goorm-popcorn-service:${IMAGE_TAG}"
    }
  ]
}
```

### 배포 스크립트에서의 사용
```bash
# Development 배포
./deploy.sh checkin-service dev dev-a1b2c3d4

# Production 배포
./deploy.sh checkin-service prod a1b2c3d4
```

## 라이프사이클 관리

### ECR 라이프사이클 정책
- **latest, stable**: 영구 보존
- **semantic version (v*)**: 최근 10개 버전 보존
- **dev-***: 30일 후 삭제
- **feature-*, hotfix-*, pr-***: 7일 후 삭제

### 수동 태그 관리
```bash
# stable 태그 추가 (프로덕션 검증 후)
aws ecr batch-get-image --repository-name goorm-popcorn-checkin --image-ids imageTag=a1b2c3d4 \
  --query 'images[0].imageManifest' --output text | \
aws ecr put-image --repository-name goorm-popcorn-checkin --image-tag stable --image-manifest file:///dev/stdin
```

## 서비스별 ECR 레포지토리

| 서비스 | ECR 레포지토리 |
|--------|----------------|
| API Gateway | goorm-popcorn-api-gateway |
| User Service | goorm-popcorn-user |
| Store Service | goorm-popcorn-store |
| Order Service | goorm-popcorn-order |
| Payment Service | goorm-popcorn-payment |
| CheckIn Service | goorm-popcorn-checkin |
| Order Query Service | goorm-popcorn-order-query |

## 모니터링 및 알림

### Discord 알림 형식
```
✅ checkin-service 배포 성공

환경: 🟡 Staging
버전: dev-a1b2c3d4
태그: dev-a1b2c3d4,dev-latest,dev-20240120
작성자: developer
브랜치: develop
실행 시간: 42번째 실행
```

## 트러블슈팅

### 일반적인 문제
1. **태그가 생성되지 않음**: 브랜치 이름 확인
2. **배포 실패**: 이미지 태그와 task definition의 IMAGE_TAG 일치 확인
3. **이미지를 찾을 수 없음**: ECR 레포지토리 이름과 태그 확인

### 디버깅 명령어
```bash
# ECR 이미지 목록 확인
aws ecr list-images --repository-name goorm-popcorn-checkin

# 특정 태그 확인
aws ecr describe-images --repository-name goorm-popcorn-checkin --image-ids imageTag=dev-latest
```