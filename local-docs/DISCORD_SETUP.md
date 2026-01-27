# Discord 웹훅 설정 가이드

## 📋 개요

GitHub Actions에서 Discord로 배포 알림을 받기 위한 설정 가이드입니다.

## 🔧 Discord 웹훅 생성

### 1. Discord 서버 설정

1. **Discord 서버에서 채널 생성**
   - 채널명: `#deployments` (또는 원하는 이름)
   - 채널 타입: 텍스트 채널

2. **채널 설정 접근**
   - 채널 우클릭 → "채널 편집"
   - 또는 채널명 옆 톱니바퀴 아이콘 클릭

### 2. 웹훅 생성

1. **통합 탭 이동**
   - 좌측 메뉴에서 "통합" 클릭

2. **웹훅 생성**
   - "웹훅" 섹션에서 "웹훅 만들기" 클릭
   - 웹훅 이름: `GitHub Actions` (또는 원하는 이름)
   - 채널: 알림을 받을 채널 선택

3. **웹훅 URL 복사**
   - "웹훅 URL 복사" 버튼 클릭
   - URL 형태: `https://discord.com/api/webhooks/1234567890/abcdefghijklmnopqrstuvwxyz`

## 🔐 GitHub Secrets 설정

### 1. GitHub 저장소 설정

1. **Settings 탭 이동**
   - GitHub 저장소 → Settings

2. **Secrets and variables 접근**
   - 좌측 메뉴 → "Secrets and variables" → "Actions"

3. **New repository secret 생성**
   - "New repository secret" 버튼 클릭
   - Name: `DISCORD_WEBHOOK`
   - Secret: 복사한 Discord 웹훅 URL 붙여넣기

### 2. 필수 Secrets 목록

```yaml
필수 Secrets:
  DISCORD_WEBHOOK: Discord 웹훅 URL
  AWS_ROLE_ARN: AWS IAM 역할 ARN
  SNYK_TOKEN: Snyk 보안 스캔 토큰
  PROD_APPROVERS: 프로덕션 승인자 목록

선택적 Secrets:
  CODECOV_TOKEN: 코드 커버리지 토큰
```

## 📱 알림 형태

### 성공 알림 예시

```
✅ user-service 배포 성공

환경: 🟡 Staging
버전: abc123def
작성자: developer-name
브랜치: develop
실행 시간: 42번째 실행
```

### 실패 알림 예시

```
❌ payment-service 배포 실패

환경: 🔴 Production
버전: xyz789abc
작성자: developer-name
브랜치: main
보안 스캔: 완료 ✅
실행 시간: 15번째 실행
```

### 통합 워크플로우 알림 예시

```
🚀 Unified CI/CD Pipeline ✅ 성공

서비스: user-service,order-service,payment-service
환경: Production
버전: commit-sha-here
작성자: developer-name
트리거: workflow_dispatch
실행 시간: 8번째 실행
```

## 🎨 서비스별 이모지

각 서비스는 고유한 이모지로 구분됩니다:

```yaml
서비스별 이모지:
  🔒 payment-service    # 보안 중요 서비스
  🚪 api-gateway       # 게이트웨이 서비스
  📱 qr-service        # QR 코드 서비스
  📊 order-query       # 조회 서비스
  ✅ 기타 서비스        # 일반 서비스
```

## 🔧 고급 설정

### 1. 멘션 추가

특정 상황에서 팀원을 멘션하려면:

```yaml
# 워크플로우 파일에서
args: |
  ${{ job.status == 'failure' && '<@USER_ID>' || '' }}
  ❌ **${{ env.SERVICE_NAME }}** 배포 실패
  
  **환경**: 🔴 Production
  **버전**: `${{ github.sha }}`
```

### 2. 조건부 알림

특정 조건에서만 알림을 보내려면:

```yaml
- name: Notify Discord (Production Only)
  if: github.ref == 'refs/heads/main'
  uses: Ilshidur/action-discord@master
  with:
    args: "프로덕션 배포 완료!"
  env:
    DISCORD_WEBHOOK: ${{ secrets.DISCORD_WEBHOOK }}
```

### 3. 임베드 메시지

더 풍부한 형태의 메시지를 보내려면:

```yaml
- name: Notify Discord with Embed
  uses: Ilshidur/action-discord@master
  with:
    args: |
      {
        "embeds": [{
          "title": "${{ env.SERVICE_NAME }} 배포 완료",
          "description": "배포가 성공적으로 완료되었습니다.",
          "color": 65280,
          "fields": [
            {"name": "환경", "value": "Production", "inline": true},
            {"name": "버전", "value": "${{ github.sha }}", "inline": true},
            {"name": "작성자", "value": "${{ github.actor }}", "inline": true}
          ]
        }]
      }
  env:
    DISCORD_WEBHOOK: ${{ secrets.DISCORD_WEBHOOK }}
```

## 🐛 문제 해결

### 일반적인 문제들

#### 1. 웹훅 URL 오류

**증상:**
```
Error: Request failed with status code 404
```

**해결 방법:**
- Discord 웹훅 URL이 올바른지 확인
- 웹훅이 삭제되지 않았는지 확인
- GitHub Secrets에 올바르게 저장되었는지 확인

#### 2. 권한 오류

**증상:**
```
Error: Request failed with status code 401
```

**해결 방법:**
- Discord 서버에서 웹훅 권한 확인
- 채널에 메시지 보내기 권한이 있는지 확인

#### 3. 메시지 길이 제한

**증상:**
```
Error: Request failed with status code 400
```

**해결 방법:**
- Discord 메시지는 2000자 제한
- 긴 메시지는 여러 개로 분할
- 불필요한 정보 제거

### 디버깅 방법

#### 1. 웹훅 테스트

```bash
# curl을 사용한 웹훅 테스트
curl -X POST "YOUR_DISCORD_WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{"content": "테스트 메시지입니다!"}'
```

#### 2. 워크플로우 로그 확인

- GitHub Actions 탭에서 실행 로그 확인
- Discord 알림 단계의 상세 로그 분석

## 📚 참고 자료

### Discord 웹훅 문서
- [Discord Webhook Guide](https://support.discord.com/hc/en-us/articles/228383668-Intro-to-Webhooks)
- [Discord API Documentation](https://discord.com/developers/docs/resources/webhook)

### GitHub Actions Discord 액션
- [Ilshidur/action-discord](https://github.com/Ilshidur/action-discord)
- [GitHub Actions Marketplace](https://github.com/marketplace/actions/actions-for-discord)

### 메시지 포맷팅
- [Discord Markdown](https://support.discord.com/hc/en-us/articles/210298617-Markdown-Text-101-Chat-Formatting-Bold-Italic-Underline-)
- [Discord Embed Visualizer](https://leovoel.github.io/embed-visualizer/)

---

**문서 버전**: 1.0  
**최종 업데이트**: 2024-01-26  
**작성자**: DevOps Team