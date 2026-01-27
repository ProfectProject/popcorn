# GitHub Actions CI/CD 설정 가이드

이 문서는 Goorm Popcorn 마이크로서비스의 GitHub Actions CI/CD 파이프라인을 설정하는 방법을 안내합니다.

## 📋 사전 요구사항

### 1. AWS 리소스
- ✅ ECS 클러스터 및 서비스 (Terraform으로 생성됨)
- ✅ ECR 리포지토리 (7개 서비스)
- ✅ IAM 역할 및 정책
- ✅ VPC, 서브넷, 보안 그룹

### 2. 외부 서비스 계정
- [ ] Snyk 계정 (보안 스캔)
- [ ] Slack 워크스페이스 (알림)
- [ ] Codecov 계정 (코드 커버리지)

## 🔐 GitHub Secrets 설정

### 1. AWS 인증 설정

#### IAM 역할 생성
```bash
# 1. GitHub Actions용 IAM 역할 생성
aws iam create-role \
  --role-name github-actions-role \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Principal": {
          "Federated": "arn:aws:iam::375896310755:oidc-provider/token.actions.githubusercontent.com"
        },
        "Action": "sts:AssumeRoleWithWebIdentity",
        "Condition": {
          "StringEquals": {
            "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
          },
          "StringLike": {
            "token.actions.githubusercontent.com:sub": "repo:ProfectProject/popcorn_msa:*"
          }
        }
      }
    ]
  }'

# 2. 필요한 정책 연결
aws iam attach-role-policy \
  --role-name github-actions-role \
  --policy-arn arn:aws:iam::aws:policy/AmazonECS_FullAccess

aws iam attach-role-policy \
  --role-name github-actions-role \
  --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryFullAccess
```

#### GitHub OIDC Provider 설정
```bash
# GitHub OIDC Provider 생성 (한 번만 실행)
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com \
  --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1
```

### 2. GitHub Repository Secrets 설정

GitHub 리포지토리 → Settings → Secrets and variables → Actions에서 다음 Secrets을 추가:

#### AWS 관련
```yaml
AWS_ROLE_ARN: arn:aws:iam::375896310755:role/github-actions-role
```

#### 보안 스캔
```yaml
SNYK_TOKEN: your-snyk-api-token
```

#### 알림
```yaml
DISCORD_WEBHOOK: https://discord.com/api/webhooks/1234567890/abcdefghijklmnopqrstuvwxyz
```

#### 승인자 (GitHub 팀 또는 사용자명)
```yaml
PROD_APPROVERS: tech-lead-team,security-team
```

## 🔧 Snyk 설정

### 1. Snyk 계정 생성
1. [Snyk 웹사이트](https://snyk.io) 방문
2. GitHub 계정으로 로그인
3. 조직 생성 또는 기존 조직 선택

### 2. API 토큰 생성
1. Snyk 대시보드 → Settings → API Token
2. "Generate token" 클릭
3. 토큰을 GitHub Secrets에 `SNYK_TOKEN`으로 추가

### 3. 프로젝트 연동
```bash
# Snyk CLI 설치
npm install -g snyk

# 인증
snyk auth

# 프로젝트 스캔 테스트
snyk test --file=users/build.gradle
```

## 📢 Slack 알림 설정

### 1. Slack App 생성
1. [Slack API](https://api.slack.com/apps) 방문
2. "Create New App" → "From scratch"
3. App 이름: "Goorm Popcorn CI/CD"
4. 워크스페이스 선택

### 2. Incoming Webhook 설정
1. Features → Incoming Webhooks → "Activate Incoming Webhooks"
2. "Add New Webhook to Workspace"
3. 채널 선택: `#deployments` (미리 생성 필요)
4. Webhook URL을 GitHub Secrets에 `SLACK_WEBHOOK`으로 추가

### 3. 채널 생성
```
채널명: #deployments
목적: CI/CD 배포 알림 전용
멤버: 개발팀, DevOps팀, Tech Lead
```

## 📊 Codecov 설정

### 1. Codecov 계정 생성
1. [Codecov](https://codecov.io) 방문
2. GitHub 계정으로 로그인
3. 리포지토리 연동

### 2. 토큰 설정 (Private 리포지토리인 경우)
1. Codecov 대시보드 → Repository → Settings
2. "Repository Upload Token" 복사
3. GitHub Secrets에 `CODECOV_TOKEN`으로 추가

## 🏷️ GitHub 브랜치 보호 설정

### 1. main 브랜치 보호
Repository → Settings → Branches → Add rule:

```yaml
Branch name pattern: main
Settings:
  ✅ Require a pull request before merging
  ✅ Require approvals (2)
  ✅ Dismiss stale PR approvals when new commits are pushed
  ✅ Require review from code owners
  ✅ Require status checks to pass before merging
  ✅ Require branches to be up to date before merging
  ✅ Require conversation resolution before merging
  ✅ Restrict pushes that create files larger than 100MB
```

### 2. develop 브랜치 보호
```yaml
Branch name pattern: develop
Settings:
  ✅ Require a pull request before merging
  ✅ Require approvals (1)
  ✅ Require status checks to pass before merging
  ✅ Require branches to be up to date before merging
```

## 👥 GitHub Teams 설정

### 1. 팀 생성
Organization → Teams → New team:

```yaml
tech-lead-team:
  - Members: Tech Lead, Senior Developers
  - Permissions: Admin
  - Purpose: 프로덕션 배포 승인

security-team:
  - Members: Security Engineer, DevOps Lead
  - Permissions: Write
  - Purpose: 보안 검토 및 승인

dev-team:
  - Members: All Developers
  - Permissions: Write
  - Purpose: 개발 및 스테이징 배포
```

### 2. CODEOWNERS 파일 생성
```bash
# .github/CODEOWNERS
# Global owners
* @tech-lead-team

# Payment service requires security review
payment/ @security-team @tech-lead-team

# Infrastructure and CI/CD
.aws/ @tech-lead-team
.github/ @tech-lead-team
terraform/ @tech-lead-team

# Task definitions
.aws/task-definitions/ @tech-lead-team @security-team
```

## 🧪 워크플로우 테스트

### 1. 기본 테스트
```bash
# 1. feature 브랜치 생성
git checkout -b feature/test-cicd

# 2. 간단한 변경사항 추가
echo "# Test" >> users/README.md
git add users/README.md
git commit -m "test: CI/CD 파이프라인 테스트"

# 3. PR 생성
git push origin feature/test-cicd
# GitHub에서 PR 생성 → 자동으로 테스트 실행 확인
```

### 2. 배포 테스트
```bash
# 1. develop 브랜치에 병합
# PR 승인 후 merge → 스테이징 자동 배포 확인

# 2. main 브랜치에 병합
# develop → main PR 생성 → 승인 후 merge → 프로덕션 수동 승인 대기
```

### 3. 다중 서비스 배포 테스트
1. GitHub Actions 탭 이동
2. "Multi-Service Deployment" 선택
3. "Run workflow" 클릭
4. 테스트 설정:
   - Services: `qr-service` (가장 간단한 서비스)
   - Environment: `dev`
   - Image tag: `latest`

## 🔍 문제 해결

### 1. AWS 인증 실패
```yaml
Error: Could not assume role with OIDC

해결방법:
1. IAM 역할의 Trust Policy 확인
2. GitHub OIDC Provider 존재 확인
3. 리포지토리 경로 정확성 확인
```

### 2. ECR 푸시 실패
```yaml
Error: no basic auth credentials

해결방법:
1. ECR 리포지토리 존재 확인
2. IAM 권한 확인 (ECR FullAccess)
3. AWS 리전 설정 확인
```

### 3. ECS 배포 실패
```yaml
Error: Service not found

해결방법:
1. ECS 클러스터 및 서비스 존재 확인
2. Task Definition 이름 일치 확인
3. 서브넷 및 보안 그룹 설정 확인
```

## 📋 체크리스트

배포 전 다음 항목들을 확인하세요:

### AWS 설정
- [ ] ECS 클러스터 생성됨
- [ ] ECR 리포지토리 7개 생성됨
- [ ] IAM 역할 및 정책 설정됨
- [ ] GitHub OIDC Provider 생성됨

### GitHub 설정
- [ ] Repository Secrets 모두 설정됨
- [ ] 브랜치 보호 규칙 적용됨
- [ ] Teams 및 CODEOWNERS 설정됨
- [ ] 워크플로우 파일 업로드됨

### 외부 서비스
- [ ] Snyk 계정 및 토큰 설정됨
- [ ] Slack 채널 및 Webhook 설정됨
- [ ] Codecov 연동 완료됨

### 테스트
- [ ] 기본 워크플로우 테스트 완료
- [ ] 스테이징 배포 테스트 완료
- [ ] 프로덕션 승인 프로세스 테스트 완료

## 🚀 다음 단계

설정 완료 후:

1. **팀 교육**: 개발팀에게 새로운 CI/CD 프로세스 교육
2. **문서화**: 팀 위키에 배포 가이드 작성
3. **모니터링**: 배포 메트릭 및 성공률 모니터링
4. **최적화**: 빌드 시간 및 배포 시간 최적화

## 📞 지원

설정 중 문제가 발생하면:

- **DevOps Team**: Slack `#devops-support`
- **Tech Lead**: 직접 연락
- **문서**: [CI/CD 아키텍처 가이드](../../popcorn-terraform/docs/cicd-architecture.md)

---

**문서 버전**: 1.0  
**최종 업데이트**: 2024-01-26  
**작성자**: DevOps Team