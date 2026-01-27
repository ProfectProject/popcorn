# 🚀 로컬에서 ECR/ECS 배포 가이드

이 가이드는 CI/CD 파이프라인 대신 로컬에서 직접 ECR로 이미지를 푸시하고 ECS에 배포하는 방법을 설명합니다.

## 📋 사전 준비사항

### 1. AWS CLI 설정
```bash
# AWS CLI 설치 확인
aws --version

# AWS 자격 증명 설정 (이미 설정되어 있다면 생략)
aws configure
```

### 2. Docker 설치 확인
```bash
docker --version
```

### 3. 환경변수 확인
`.env` 파일이 terraform output을 기반으로 업데이트되었습니다:
- **RDS 엔드포인트**: `goorm-popcorn-dev-postgres.cds4g0gykt3t.ap-northeast-2.rds.amazonaws.com:5432`
- **Redis 엔드포인트**: `goorm-popcorn-cache-dev.mkltth.ng.0001.apn2.cache.amazonaws.com:6379`
- **서비스 디스커버리**: CloudMap 네임스페이스 `goormpopcorn.local` 사용

## 🎯 배포 방법

### 방법 1: 개별 서비스 배포
```bash
# users 서비스 배포
./scripts/deploy-to-ecr.sh users latest

# gateway 서비스 배포
./scripts/deploy-to-ecr.sh gateway latest

# 특정 태그로 배포
./scripts/deploy-to-ecr.sh users v1.0.0
```

### 방법 2: 배치 배포 (권장)
```bash
# users와 gateway를 한번에 배포
./scripts/deploy-batch.sh latest

# 특정 태그로 배치 배포
./scripts/deploy-batch.sh v1.0.0
```

## 📊 배포 상태 확인

### ECS 서비스 상태 확인
```bash
./scripts/check-ecs-status.sh
```

### AWS 콘솔에서 확인
- **ECS 클러스터**: `goorm-popcorn-dev-cluster`
- **ALB 엔드포인트**: `goorm-popcorn-alb-dev-1916229086.ap-northeast-2.elb.amazonaws.com`

## 🔧 ECR 리포지토리 정보

| 서비스 | ECR 리포지토리 | ECS 서비스 |
|--------|---------------|------------|
| users | goorm-popcorn-user | goorm-popcorn-dev-user-service |
| gateway | goorm-popcorn-api-gateway | goorm-popcorn-dev-api-gateway |

## 🌐 서비스 디스커버리

서비스 간 통신은 AWS Cloud Map을 통해 이루어집니다:
- **네임스페이스**: `goormpopcorn.local`
- **서비스 URL 형식**: `http://[service-name].goormpopcorn.local:[port]`

예시:
- users: `http://user-service.goormpopcorn.local:8082`
- gateway: `http://api-gateway.goormpopcorn.local:8080`

## 🔍 트러블슈팅

### 1. ECR 로그인 실패
```bash
# ECR 로그인 재시도
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin 375896310755.dkr.ecr.ap-northeast-2.amazonaws.com
```

### 2. Docker 빌드 실패
```bash
# 캐시 없이 빌드
docker build --no-cache -f users/Dockerfile -t goorm-popcorn-user:latest .
```

### 3. ECS 서비스 상태 확인
```bash
# 특정 서비스 로그 확인
aws logs tail /ecs/goorm-popcorn-dev-user-service --follow --region ap-northeast-2

# 서비스 이벤트 확인
aws ecs describe-services --cluster goorm-popcorn-dev-cluster --services goorm-popcorn-dev-user-service --region ap-northeast-2 --query 'services[0].events'
```

### 4. 헬스체크 실패
ALB 헬스체크 엔드포인트 확인:
- users: `GET /actuator/health`
- gateway: `GET /actuator/health`

## 📝 배포 플로우

1. **이미지 빌드**: Dockerfile을 사용하여 Docker 이미지 생성
2. **ECR 푸시**: 빌드된 이미지를 ECR 리포지토리에 푸시
3. **ECS 업데이트**: ECS 서비스에 새로운 배포 강제 실행
4. **상태 확인**: 서비스가 안정 상태가 될 때까지 대기

## 🎉 배포 완료 후 확인사항

- [ ] ECS 서비스가 `RUNNING` 상태인지 확인
- [ ] ALB 헬스체크가 통과하는지 확인
- [ ] 서비스 로그에 에러가 없는지 확인
- [ ] API 엔드포인트가 정상 응답하는지 확인

## 🔗 유용한 링크

- **ALB 엔드포인트**: https://goorm-popcorn-alb-dev-1916229086.ap-northeast-2.elb.amazonaws.com
- **AWS ECS 콘솔**: https://ap-northeast-2.console.aws.amazon.com/ecs/home?region=ap-northeast-2#/clusters/goorm-popcorn-dev-cluster
- **AWS ECR 콘솔**: https://ap-northeast-2.console.aws.amazon.com/ecr/repositories?region=ap-northeast-2