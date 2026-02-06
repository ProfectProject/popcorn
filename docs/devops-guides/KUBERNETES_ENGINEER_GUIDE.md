# Kubernetes Engineer 작업 가이드

## 🎯 역할 개요
Kubernetes Engineer는 EKS 클러스터 운영과 Popcorn MSA의 7개 마이크로서비스 배포 및 관리를 담당합니다.

## 📋 일일 체크리스트

### 아침 (09:00 - 10:00)
- [ ] EKS 클러스터 상태 확인 (`kubectl get nodes`)
- [ ] 모든 네임스페이스의 Pod 상태 점검
- [ ] PVC 및 스토리지 사용량 확인
- [ ] 클러스터 리소스 사용률 모니터링
- [ ] 밤사이 발생한 Kubernetes 이벤트 검토

### 오전 (10:00 - 12:00)
- [ ] 배포 대기 중인 서비스 확인
- [ ] HPA/VPA 스케일링 상태 점검
- [ ] 클러스터 오토스케일러 동작 확인
- [ ] 서비스 메시 트래픽 분석 (향후 Istio 도입 대비)
- [ ] 보안 정책 및 RBAC 검토

### 오후 (13:00 - 18:00)
- [ ] 새로운 서비스 배포 또는 업데이트
- [ ] Helm Chart 개발 및 유지보수
- [ ] 성능 최적화 작업
- [ ] 문서 업데이트 및 Runbook 작성
- [ ] 팀원 지원 및 기술 공유

## ☸️ EKS 클러스터 관리

### 클러스터 상태 점검 스크립트
```bash
#!/bin/bash
# daily-cluster-check.sh

echo "🔍 EKS 클러스터 일일 점검"
echo "=========================="

# 클러스터 정보
echo "📊 클러스터 정보:"
kubectl cluster-info

# 노드 상태
echo -e "\n🖥️ 노드 상태:"
kubectl get nodes -o wide

# 네임스페이스별 리소스 사용량
echo -e "\n📈 네임스페이스별 리소스:"
kubectl top nodes
kubectl top pods --all-namespaces --sort-by=cpu

# 문제가 있는 Pod 확인
echo -e "\n⚠️ 문제 Pod 확인:"
kubectl get pods --all-namespaces --field-selector=status.phase!=Running

# PVC 상태
echo -e "\n💾 스토리지 상태:"
kubectl get pvc --all-namespaces

# 최근 이벤트
echo -e "\n📋 최근 이벤트:"
kubectl get events --all-namespaces --sort-by='.lastTimestamp' | tail -20

echo -e "\n✅ 클러스터 점검 완료"
```

### 노드 그룹 관리 체크리스트
```yaml
Daily Checks:
  - [ ] 노드 상태 및 가용성 확인
  - [ ] 노드별 리소스 사용률 모니터링
  - [ ] Spot Instance 중단 알림 확인
  - [ ] 노드 그룹 스케일링 이벤트 검토

Weekly Tasks:
  - [ ] 노드 그룹 설정 최적화 검토
  - [ ] AMI 업데이트 계획 수립
  - [ ] 노드 라벨 및 테인트 정책 검토
  - [ ] 리소스 요청/제한 최적화

Monthly Tasks:
  - [ ] 클러스터 버전 업그레이드 계획
  - [ ] 노드 그룹 인스턴스 타입 최적화
  - [ ] 비용 분석 및 최적화
  - [ ] 보안 패치 적용 계획
```

## 🚀 서비스 배포 관리

### Popcorn MSA 서비스 배포 체크리스트
```yaml
Pre-deployment:
  - [ ] 이미지 태그 및 버전 확인
  - [ ] ConfigMap/Secret 업데이트 확인
  - [ ] 리소스 요청/제한 검토
  - [ ] 헬스체크 엔드포인트 확인
  - [ ] 의존성 서비스 상태 확인

Deployment:
  - [ ] Rolling Update 전략 설정
  - [ ] 배포 진행 상황 모니터링
  - [ ] Pod 상태 및 로그 확인
  - [ ] 서비스 엔드포인트 테스트
  - [ ] 트래픽 라우팅 확인

Post-deployment:
  - [ ] 메트릭 및 알림 확인
  - [ ] 성능 지표 모니터링
  - [ ] 에러율 및 응답시간 검토
  - [ ] 롤백 계획 준비
  - [ ] 배포 결과 문서화
```

### 서비스별 배포 가이드

**Gateway Service (Spring Cloud Gateway)**:
```yaml
Deployment Strategy: RollingUpdate
Replicas: 2-5 (HPA 기반)
Resources:
  requests:
    cpu: 250m
    memory: 256Mi
  limits:
    cpu: 500m
    memory: 512Mi

Health Checks:
  liveness: /actuator/health/liveness
  readiness: /actuator/health/readiness
  startup: /actuator/health

Environment Variables:
  - SPRING_PROFILES_ACTIVE: k8s
  - USER_SERVICE_URL: http://user-service:8082
  - ORDER_SERVICE_URL: http://order-service:8084
  - PAYMENT_SERVICE_URL: http://payment-service:8085
```

**Order Service (비즈니스 크리티컬)**:
```yaml
Deployment Strategy: RollingUpdate
Replicas: 3-10 (HPA 기반)
Resources:
  requests:
    cpu: 500m
    memory: 512Mi
  limits:
    cpu: 1000m
    memory: 1Gi

Scaling Policy:
  CPU: 75% 임계값
  Memory: 80% 임계값
  Min Replicas: 3
  Max Replicas: 10

Database Connection:
  - Connection Pool: 20
  - Timeout: 30s
  - Retry: 3회
```

**Payment Service (높은 안정성)**:
```yaml
Deployment Strategy: RollingUpdate (maxUnavailable: 1)
Replicas: 2-6 (보수적 스케일링)
Resources:
  requests:
    cpu: 500m
    memory: 512Mi
  limits:
    cpu: 1000m
    memory: 1Gi

Special Considerations:
  - 결제 중 Pod 종료 방지
  - Graceful Shutdown: 60초
  - PDB (Pod Disruption Budget) 설정
  - 외부 API 타임아웃: 30초
```

## 📊 모니터링 및 관찰성

### Kubernetes 메트릭 모니터링
```bash
# 리소스 사용률 모니터링 스크립트
#!/bin/bash

echo "📊 Kubernetes 리소스 모니터링"
echo "=============================="

# 네임스페이스별 리소스 사용량
for ns in popcorn kube-system monitoring; do
    echo -e "\n📁 Namespace: $ns"
    kubectl top pods -n $ns --sort-by=cpu | head -10
done

# HPA 상태
echo -e "\n🔄 HPA 상태:"
kubectl get hpa -n popcorn

# VPA 상태 (설치된 경우)
echo -e "\n📈 VPA 상태:"
kubectl get vpa -n popcorn 2>/dev/null || echo "VPA not installed"

# 클러스터 오토스케일러 상태
echo -e "\n🏗️ 클러스터 오토스케일러:"
kubectl get nodes --show-labels | grep -E "node-lifecycle|instance-type"

# 스토리지 사용량
echo -e "\n💾 스토리지 사용량:"
kubectl get pvc -n popcorn -o custom-columns=NAME:.metadata.name,STATUS:.status.phase,CAPACITY:.status.capacity.storage,STORAGECLASS:.spec.storageClassName
```

### 서비스 헬스체크 스크립트
```bash
#!/bin/bash
# service-health-check.sh

NAMESPACE="popcorn"
SERVICES=("gateway-service" "user-service" "order-service" "payment-service" "store-service" "checkin-service" "orderquery-service")

echo "🏥 서비스 헬스체크"
echo "=================="

for service in "${SERVICES[@]}"; do
    echo -e "\n🔍 $service 상태:"
    
    # Pod 상태
    kubectl get pods -n $NAMESPACE -l app=$service
    
    # 서비스 엔드포인트
    kubectl get endpoints -n $NAMESPACE $service
    
    # 헬스체크 (포트 포워딩 사용)
    kubectl port-forward -n $NAMESPACE svc/$service 8080:8080 &
    PF_PID=$!
    sleep 2
    
    if curl -f -s http://localhost:8080/actuator/health > /dev/null; then
        echo "✅ $service 헬스체크 성공"
    else
        echo "❌ $service 헬스체크 실패"
    fi
    
    kill $PF_PID 2>/dev/null
    sleep 1
done
```

## 🔧 문제 해결 가이드

### 일반적인 문제 및 해결방법

**Pod가 Pending 상태인 경우**:
```bash
# 1. Pod 상태 확인
kubectl describe pod <pod-name> -n popcorn

# 2. 노드 리소스 확인
kubectl top nodes
kubectl describe nodes

# 3. PVC 상태 확인
kubectl get pvc -n popcorn

# 4. 스케줄링 이벤트 확인
kubectl get events -n popcorn --sort-by='.lastTimestamp'
```

**Pod가 CrashLoopBackOff인 경우**:
```bash
# 1. 로그 확인
kubectl logs <pod-name> -n popcorn --previous

# 2. 컨테이너 상태 확인
kubectl describe pod <pod-name> -n popcorn

# 3. 리소스 제한 확인
kubectl get pod <pod-name> -n popcorn -o yaml | grep -A 10 resources

# 4. 헬스체크 설정 확인
kubectl get pod <pod-name> -n popcorn -o yaml | grep -A 20 livenessProbe
```

**서비스 연결 문제**:
```bash
# 1. 서비스 엔드포인트 확인
kubectl get endpoints -n popcorn

# 2. DNS 해석 테스트
kubectl run -it --rm debug --image=busybox --restart=Never -- nslookup user-service.popcorn.svc.cluster.local

# 3. 네트워크 정책 확인
kubectl get networkpolicies -n popcorn

# 4. 서비스 포트 확인
kubectl get svc -n popcorn -o wide
```

### 성능 최적화 체크리스트
```yaml
Resource Optimization:
  - [ ] CPU/Memory 요청값 적정성 검토
  - [ ] 리소스 제한값 최적화
  - [ ] JVM 힙 크기 조정 (Java 서비스)
  - [ ] 커넥션 풀 크기 최적화

Scaling Optimization:
  - [ ] HPA 메트릭 및 임계값 조정
  - [ ] VPA 권장사항 적용
  - [ ] 클러스터 오토스케일러 설정 최적화
  - [ ] Pod Disruption Budget 설정

Network Optimization:
  - [ ] 서비스 메시 준비 (Istio 도입 대비)
  - [ ] Ingress 설정 최적화
  - [ ] DNS 캐싱 설정
  - [ ] 네트워크 정책 최적화
```

## 📚 Helm Chart 관리

### Chart 구조 예시
```
popcorn-msa/
├── Chart.yaml
├── values.yaml
├── values-dev.yaml
├── values-prod.yaml
└── templates/
    ├── deployments/
    │   ├── gateway-deployment.yaml
    │   ├── user-service-deployment.yaml
    │   └── ...
    ├── services/
    │   └── *.yaml
    ├── configmaps/
    │   └── *.yaml
    ├── secrets/
    │   └── *.yaml
    ├── hpa/
    │   └── *.yaml
    └── ingress/
        └── *.yaml
```

### Chart 배포 스크립트
```bash
#!/bin/bash
# helm-deploy.sh

ENVIRONMENT=${1:-dev}
NAMESPACE="popcorn"
CHART_PATH="./helm/popcorn-msa"

echo "🚀 Helm Chart 배포: $ENVIRONMENT"
echo "================================"

# 네임스페이스 생성
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# Helm 배포
helm upgrade --install popcorn-msa $CHART_PATH \
    --namespace $NAMESPACE \
    --values $CHART_PATH/values-$ENVIRONMENT.yaml \
    --wait \
    --timeout 10m

# 배포 상태 확인
echo -e "\n📊 배포 상태:"
kubectl get pods -n $NAMESPACE
kubectl get svc -n $NAMESPACE
kubectl get ingress -n $NAMESPACE

echo -e "\n✅ 배포 완료"
```

## 🔐 보안 관리

### RBAC 설정 체크리스트
```yaml
Service Accounts:
  - [ ] 각 서비스별 ServiceAccount 생성
  - [ ] 최소 권한 원칙 적용
  - [ ] 토큰 자동 마운트 비활성화 (필요시)

Roles & RoleBindings:
  - [ ] 네임스페이스별 Role 정의
  - [ ] 개발팀 접근 권한 설정
  - [ ] 모니터링 시스템 권한 설정
  - [ ] CI/CD 파이프라인 권한 설정

Pod Security:
  - [ ] Pod Security Standards 적용
  - [ ] Security Context 설정
  - [ ] 루트 권한 실행 금지
  - [ ] 읽기 전용 루트 파일시스템
```

### 네트워크 보안
```yaml
Network Policies:
  - [ ] 기본 거부 정책 설정
  - [ ] 서비스 간 통신 허용 규칙
  - [ ] 외부 트래픽 제한
  - [ ] 모니터링 시스템 접근 허용

Ingress Security:
  - [ ] TLS 인증서 설정
  - [ ] Rate Limiting 설정
  - [ ] IP 화이트리스트 (필요시)
  - [ ] WAF 규칙 적용
```

## 📖 문서화 및 Runbook

### 운영 Runbook 템플릿
```markdown
# [서비스명] 운영 Runbook

## 서비스 개요
- 목적: [서비스 목적]
- 포트: [포트 번호]
- 의존성: [의존 서비스들]
- 중요도: [P0/P1/P2/P3]

## 배포 정보
- 이미지: [ECR 레포지토리]
- 설정: [ConfigMap/Secret]
- 리소스: [CPU/Memory 요구사항]
- 스케일링: [HPA 설정]

## 모니터링
- 헬스체크: [엔드포인트]
- 주요 메트릭: [모니터링 지표]
- 알림 임계값: [알림 설정]
- 대시보드: [Grafana 링크]

## 문제 해결
### 일반적인 문제
1. [문제 상황]
   - 증상: [증상 설명]
   - 원인: [가능한 원인]
   - 해결: [해결 방법]

### 긴급 대응
- 서비스 재시작: `kubectl rollout restart deployment/[서비스명] -n popcorn`
- 로그 확인: `kubectl logs -f deployment/[서비스명] -n popcorn`
- 스케일링: `kubectl scale deployment/[서비스명] --replicas=5 -n popcorn`

## 연락처
- 담당자: [이름]
- 온콜: [온콜 정보]
- 에스컬레이션: [에스컬레이션 경로]
```

## 🎯 성공을 위한 팁

1. **자동화 우선**: 반복적인 작업은 스크립트로 자동화
2. **모니터링 강화**: 문제를 사전에 감지할 수 있는 모니터링 구축
3. **문서화 습관**: 모든 변경사항과 해결방법을 문서화
4. **보안 의식**: 항상 보안을 고려한 설정과 배포
5. **지속적 학습**: Kubernetes 생태계의 새로운 기능과 베스트 프랙티스 학습

Kubernetes Engineer로서 안정적이고 확장 가능한 컨테이너 플랫폼을 구축하고 운영하는 것이 핵심 목표입니다. 이 가이드를 참고하여 효과적으로 EKS 클러스터와 Popcorn MSA 서비스들을 관리하시기 바랍니다.