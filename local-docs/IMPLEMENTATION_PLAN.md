# 🔐 시크릿 관리 구현 계획

## 📋 개요

이 문서는 하드코딩된 시크릿을 AWS Secrets Manager로 마이그레이션하고 보안 개선을 완료하기 위한 단계별 구현 계획을 설명합니다.

## 🎯 구현 단계

### 1단계: 중요 보안 수정 (즉시 - 오늘)
**우선순위**: 🔴 치명적  
**일정**: 1-2시간  
**영향**: 높음 (서비스 재시작 필요)

#### 작업:
1. **마이그레이션 스크립트 실행**
   ```bash
   cd popcorn_msa
   chmod +x .github/scripts/migrate-task-definition-secrets.sh
   ./.github/scripts/migrate-task-definition-secrets.sh
   ```

2. **AWS 시크릿 생성**
   - 개발/프로덕션 환경용 JWT 시크릿
   - 향상된 데이터베이스 자격증명
   - Redis 인증 토큰
   - 내부 API 키

3. **태스크 정의 업데이트**
   - 원본 파일을 개선된 버전으로 교체
   - JSON 구문 검증
   - 개발 환경에서 테스트

4. **배포 및 검증**
   - 개발 환경에 먼저 배포
   - 애플리케이션 기능 확인
   - CloudWatch 로그에서 시크릿 접근 확인
   - 검증 후 프로덕션 배포

### 2단계: 보안 검증 (24시간 내)
**우선순위**: 🟡 높음  
**일정**: 2-4시간  
**영향**: 낮음 (모니터링만)

#### 작업:
1. **보안 스캔 실행**
   ```bash
   # 시크릿 스캔 워크플로우 트리거
   gh workflow run secret-scan.yml
   ```

2. **시크릿 접근 검증**
   - ECS 태스크 로그에서 시크릿 로딩 확인
   - 로그에 하드코딩된 값이 없는지 확인
   - 애플리케이션 인증 테스트

3. **모니터링 및 알림**
   - 시크릿 접근에 대한 CloudWatch 알람 설정
   - 보안 이벤트에 대한 Discord 알림 설정

### 3단계: 추가 보안 강화 (1주일 내)
**우선순위**: 🟢 중간  
**일정**: 1-2일  
**영향**: 낮음 (점진적 배포)

#### 작업:
1. **시크릿 로테이션 구현**
   - JWT 시크릿 자동 로테이션 설정
   - 데이터베이스 비밀번호 로테이션 설정
   - 로테이션 모니터링 생성

2. **서비스 간 인증**
   - 내부 통신용 API 키 추가
   - 요청 서명 구현
   - 서비스 설정 업데이트

## 🔧 상세 구현 단계

### 1단계: 마이그레이션 전 체크리스트

- [ ] **AWS CLI 설정** 적절한 권한으로
- [ ] **기존 태스크 정의 백업**
- [ ] **AWS Secrets Manager 권한 확인**
- [ ] **개발 환경에서 스크립트 테스트**

### 2단계: 마이그레이션 스크립트 실행

마이그레이션 스크립트는 다음을 수행합니다:
1. AWS Secrets Manager에 향상된 데이터베이스 자격증명 생성
2. 각 환경에 대한 보안 JWT 시크릿 생성
3. Redis 인증 토큰 생성
4. 내부 API 키 생성
5. 시크릿 참조로 태스크 정의 업데이트
6. 원본 파일 백업 생성

### 3단계: 검증 명령어

```bash
# 1. 시크릿이 생성되었는지 확인
aws secretsmanager list-secrets --query 'SecretList[?contains(Name, `goorm-popcorn`)].Name'

# 2. 시크릿 접근 테스트
aws secretsmanager get-secret-value --secret-id "goorm-popcorn-dev/jwt-secret" --query 'SecretString'

# 3. 태스크 정의 구문 검증
aws ecs describe-task-definition --task-definition goorm-popcorn-dev-user-service --query 'taskDefinition.containerDefinitions[0].secrets'

# 4. 배포 후 애플리케이션 로그 확인
aws logs filter-log-events --log-group-name "/aws/ecs/goorm-popcorn-dev/user-service" --start-time $(date -d '5 minutes ago' +%s)000
```

### 4단계: 롤백 계획 (필요시)

```bash
# 1. 원본 태스크 정의 복원
cp .aws/task-definitions/backup-*/user-service.json .aws/task-definitions/
cp .aws/task-definitions/backup-*/backend-service.json .aws/task-definitions/

# 2. 원본 설정으로 재배포
# 3. 문제 조사 및 재시도 전 수정
```

## 📊 예상 결과

### 보안 개선사항
- ✅ JWT 시크릿이 AWS Secrets Manager로 이동
- ✅ 데이터베이스 연결 정보 보호
- ✅ 환경별 시크릿 격리
- ✅ 자동 시크릿 스캔 활성화
- ✅ 로테이션 기능 구현

### 운영상 이점
- 🔄 중앙 집중식 시크릿 관리
- 📊 시크릿 접근 모니터링
- 🔐 자동 로테이션 지원
- 🚨 보안 사고 탐지
- 📋 컴플라이언스 감사 추적

## 🚨 위험 평가

### 높은 위험 항목
- **서비스 다운타임**: 태스크 정의 변경으로 서비스 재시작 필요
- **시크릿 접근 실패**: 잘못된 ARN으로 서비스 시작 실패 가능
- **권한 문제**: ECS 태스크에 Secrets Manager 권한 필요

### 완화 전략
- **블루-그린 배포**: 개발 환경에서 먼저 테스트
- **점진적 배포**: 한 번에 하나의 서비스씩
- **빠른 롤백**: 원본 설정을 백업으로 보관
- **모니터링**: 실패에 대한 실시간 알림

## 📋 구현 후 체크리스트

### 즉시 (1시간 내)
- [ ] 모든 서비스가 성공적으로 시작됨
- [ ] 로그에 하드코딩된 시크릿 없음
- [ ] 인증이 정상적으로 작동
- [ ] 데이터베이스 연결 설정됨
- [ ] Redis 연결 작동

### 단기 (24시간 내)
- [ ] 보안 스캔에서 하드코딩된 시크릿 없음 확인
- [ ] CloudWatch 로그에서 적절한 시크릿 로딩 확인
- [ ] 애플리케이션 기능 완전 테스트
- [ ] 성능 영향 평가
- [ ] 문서 업데이트

### 장기 (1주일 내)
- [ ] 시크릿 로테이션 테스트
- [ ] 모니터링 대시보드 생성
- [ ] 팀 교육 완료
- [ ] 사고 대응 절차 업데이트
- [ ] 컴플라이언스 감사 통과

## 🔗 관련 리소스

- [마이그레이션 스크립트](./scripts/migrate-task-definition-secrets.sh)
- [태스크 정의 시크릿 가이드](./TASK_DEFINITION_SECRETS.md)
- [보안 스캔 워크플로우](../workflows/secret-scan.yml)
- [AWS Secrets Manager 문서](https://docs.aws.amazon.com/secretsmanager/)

---

**문서 버전**: 1.0  
**생성일**: 2024-01-26  
**작성자**: DevOps 팀  
**상태**: 구현 준비 완료