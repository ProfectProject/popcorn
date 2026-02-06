# Popcorn MSA DevOps/SRE 팀 역할 분담 및 책임

## 📋 프로젝트 개요

**서비스**: Popcorn MSA (팝업 이벤트 이커머스 플랫폼)  
**아키텍처**: EKS 기반 마이크로서비스 (Istio 미사용)  
**인프라**: AWS 기반 Terraform IaC  
**서비스 구성**: 7개 마이크로서비스 + API Gateway  

---

## 🎯 DevOps/SRE 팀 구성 및 역할

### 팀 구성 (5-7명 권장)

```
DevOps/SRE Team (5-7명)
├── Platform Engineer (1명) - 팀 리드
├── Infrastructure Engineer (1-2명)
├── Kubernetes Engineer (1명)
├── CI/CD Engineer (1명)
├── Monitoring & Observability Engineer (1명)
└── Security Engineer (1명) - 겸직 가능
```

---

## 👥 역할별 상세 책임

### 1. 🏗️ Platform Engineer (팀 리드)

**주요 책임**: 전체 플랫폼 아키텍처 설계 및 팀 관리

**핵심 업무**:
- 전체 인프라 아키텍처 설계 및 의사결정
- 기술 스택 선정 및 표준화
- 팀 간 협업 및 커뮤니케이션 조율
- 비용 최적화 및 성능 튜닝 전략 수립
- 장애 대응 총괄 및 포스트모템 주도

**담당 영역**:
```yaml
Architecture:
  - 전체 시스템 설계 검토
  - 마이크로서비스 간 통신 패턴 정의
  - 확장성 및 가용성 전략 수립

Management:
  - 팀 업무 분배 및 일정 관리
  - 개발팀과의 요구사항 조율
  - 예산 관리 및 비용 최적화

Standards:
  - 코딩 표준 및 베스트 프랙티스 정의
  - 문서화 표준 수립
  - 보안 정책 및 컴플라이언스 관리
```

**필요 스킬**:
- AWS 아키텍처 설계 (Solutions Architect 수준)
- Kubernetes 및 컨테이너 오케스트레이션
- 마이크로서비스 아키텍처 패턴
- 팀 리더십 및 프로젝트 관리
- 비용 최적화 및 성능 튜닝

**주요 도구**:
- AWS Well-Architected Framework
- Terraform Enterprise
- Kubernetes
- Grafana/Prometheus
- Slack/Jira

---

### 2. 🏢 Infrastructure Engineer (1-2명)

**주요 책임**: AWS 인프라 구축 및 관리

**핵심 업무**:
- Terraform 코드 작성 및 유지보수
- AWS 리소스 프로비저닝 및 관리
- 네트워킹 및 보안 그룹 설정
- 데이터베이스 관리 (RDS, ElastiCache)
- 백업 및 재해 복구 계획 수립

**담당 영역**:
```yaml
Infrastructure as Code:
  - Terraform 모듈 개발 및 관리
  - 환경별 인프라 구성 (dev/staging/prod)
  - 상태 파일 관리 및 백엔드 설정

AWS Services:
  - VPC, Subnet, Route Table 관리
  - ALB, NLB 설정 및 최적화
  - RDS PostgreSQL 관리 (Multi-AZ, 백업)
  - ElastiCache Valkey 클러스터 관리
  - EC2 Kafka 클러스터 운영

Networking:
  - VPC Peering 및 Transit Gateway
  - Route 53 DNS 관리
  - CloudFront CDN 설정
  - VPC Endpoints 구성

Security:
  - Security Groups 및 NACLs 관리
  - IAM 역할 및 정책 설정
  - Secrets Manager 및 Parameter Store
  - AWS Config 및 CloudTrail 설정
```

**필요 스킬**:
- Terraform 고급 사용법 (모듈, 상태 관리)
- AWS 네트워킹 (VPC, Route 53, CloudFront)
- 데이터베이스 관리 (PostgreSQL, Redis)
- Linux 시스템 관리
- 보안 베스트 프랙티스

**주요 도구**:
- Terraform/Terragrunt
- AWS CLI/Console
- Ansible (설정 관리)
- Packer (AMI 빌드)
- Vault (시크릿 관리)

**업무 분담 (2명인 경우)**:
- **Senior Infrastructure Engineer**: 아키텍처 설계, 복잡한 네트워킹, 보안
- **Infrastructure Engineer**: 일상 운영, 모니터링, 백업 관리

---

### 3. ☸️ Kubernetes Engineer

**주요 책임**: EKS 클러스터 및 Kubernetes 워크로드 관리

**핵심 업무**:
- EKS 클러스터 구축 및 운영
- Kubernetes 매니페스트 작성 및 관리
- 서비스 배포 및 스케일링 관리
- 네임스페이스 및 RBAC 설정
- Helm Chart 개발 및 관리

**담당 영역**:
```yaml
EKS Management:
  - 클러스터 생성 및 업그레이드
  - 노드 그룹 관리 (Managed/Self-managed)
  - Add-ons 관리 (AWS Load Balancer Controller, EBS CSI)
  - 클러스터 오토스케일러 설정

Workload Management:
  - Deployment, Service, Ingress 관리
  - ConfigMap, Secret 관리
  - PersistentVolume 및 StorageClass 설정
  - HPA/VPA 설정 및 튜닝

Service Mesh (미래):
  - Istio 도입 준비 및 계획
  - 서비스 간 통신 패턴 분석
  - 트래픽 관리 전략 수립

Application Deployment:
  - 7개 마이크로서비스 배포 관리
  - Rolling Update 및 Blue/Green 배포
  - Canary 배포 전략 수립
  - 서비스 디스커버리 설정
```

**필요 스킬**:
- Kubernetes 고급 운영 (CKA/CKAD 수준)
- EKS 전문 지식
- Helm Chart 개발
- 컨테이너 보안
- 네트워킹 (CNI, Service Mesh)

**주요 도구**:
- kubectl/kubectx/kubens
- Helm/Helmfile
- Kustomize
- Lens/K9s
- ArgoCD/Flux

---

### 4. 🚀 CI/CD Engineer

**주요 책임**: 지속적 통합/배포 파이프라인 구축 및 관리

**핵심 업무**:
- GitHub Actions 워크플로우 설계 및 구현
- ArgoCD GitOps 파이프라인 구축
- 컨테이너 이미지 빌드 및 관리
- 배포 자동화 및 롤백 전략
- 코드 품질 및 보안 스캔 통합

**담당 영역**:
```yaml
CI Pipeline:
  - GitHub Actions 워크플로우 작성
  - 멀티 서비스 빌드 최적화
  - 테스트 자동화 (Unit, Integration)
  - 코드 품질 검사 (SonarQube, CodeClimate)
  - 보안 스캔 (Snyk, Trivy)

CD Pipeline:
  - ArgoCD 설치 및 구성
  - GitOps 레포지토리 관리
  - 환경별 배포 전략 (dev/staging/prod)
  - 자동 롤백 및 카나리 배포
  - 배포 승인 워크플로우

Container Management:
  - ECR 레포지토리 관리
  - 이미지 태깅 전략
  - 이미지 스캔 및 취약점 관리
  - 멀티 아키텍처 빌드 (ARM64/AMD64)

Release Management:
  - 버전 관리 및 태깅 전략
  - 릴리즈 노트 자동 생성
  - 환경별 배포 스케줄링
  - 핫픽스 배포 프로세스
```

**필요 스킬**:
- GitHub Actions 고급 사용법
- ArgoCD/GitOps 패턴
- Docker/컨테이너 기술
- 스크립팅 (Bash, Python)
- 보안 스캔 도구

**주요 도구**:
- GitHub Actions
- ArgoCD/Flux
- Docker/Buildx
- ECR/Harbor
- SonarQube/Snyk

---

### 5. 📊 Monitoring & Observability Engineer

**주요 책임**: 시스템 모니터링, 로깅, 알림 시스템 구축

**핵심 업무**:
- Prometheus/Grafana 모니터링 스택 구축
- 로그 집계 및 분석 시스템 구축
- 알림 및 온콜 시스템 설정
- SLI/SLO 정의 및 모니터링
- 성능 분석 및 최적화

**담당 영역**:
```yaml
Metrics & Monitoring:
  - Prometheus 클러스터 구축 및 관리
  - Grafana 대시보드 개발
  - AlertManager 알림 규칙 설정
  - 커스텀 메트릭 수집 (JMX, Spring Actuator)
  - 인프라 모니터링 (Node Exporter, cAdvisor)

Logging:
  - ELK Stack 또는 Loki 구축
  - 로그 수집 및 파싱 (Fluent Bit/Fluentd)
  - 로그 보존 정책 및 아카이빙
  - 로그 기반 알림 설정
  - 구조화된 로깅 표준 정의

Tracing:
  - Jaeger 분산 추적 시스템 구축
  - OpenTelemetry 계측 지원
  - 성능 병목 지점 분석
  - 서비스 의존성 맵핑

Alerting:
  - PagerDuty/Slack 통합
  - 온콜 로테이션 관리
  - 알림 피로도 최소화
  - 에스컬레이션 정책 수립

SRE Practices:
  - SLI/SLO 정의 및 측정
  - Error Budget 관리
  - 포스트모템 프로세스 구축
  - 카오스 엔지니어링 도입
```

**필요 스킬**:
- Prometheus/Grafana 전문 지식
- ELK Stack 또는 Loki
- 분산 추적 (Jaeger, Zipkin)
- 통계 및 데이터 분석
- SRE 방법론

**주요 도구**:
- Prometheus/Grafana
- ELK Stack/Loki
- Jaeger/Zipkin
- PagerDuty/Opsgenie
- Chaos Monkey/Litmus

---

### 6. 🔒 Security Engineer (겸직 가능)

**주요 책임**: 보안 정책 수립 및 컴플라이언스 관리

**핵심 업무**:
- 보안 정책 및 표준 수립
- 취약점 스캔 및 관리
- 컴플라이언스 준수 (SOC2, ISO27001)
- 보안 사고 대응
- 보안 교육 및 인식 제고

**담당 영역**:
```yaml
Infrastructure Security:
  - IAM 정책 및 역할 관리
  - Security Groups 및 NACLs 최적화
  - VPC 보안 설정
  - 암호화 키 관리 (KMS)
  - 네트워크 보안 (WAF, Shield)

Application Security:
  - 컨테이너 이미지 스캔
  - 코드 보안 스캔 (SAST/DAST)
  - 의존성 취약점 관리
  - 시크릿 관리 (Secrets Manager)
  - API 보안 (Rate Limiting, Authentication)

Compliance:
  - 보안 감사 및 평가
  - 컴플라이언스 보고서 작성
  - 보안 정책 문서화
  - 직원 보안 교육
  - 사고 대응 계획 수립

Monitoring:
  - 보안 이벤트 모니터링
  - 침입 탐지 시스템 (IDS/IPS)
  - 로그 분석 및 이상 탐지
  - 보안 대시보드 구축
```

**필요 스킬**:
- AWS 보안 서비스 (IAM, KMS, WAF)
- 컨테이너 보안
- 네트워크 보안
- 컴플라이언스 프레임워크
- 보안 도구 (Snyk, Aqua, Twistlock)

**주요 도구**:
- AWS Security Hub
- Snyk/Aqua Security
- Falco/OPA Gatekeeper
- Vault/Secrets Manager
- OWASP ZAP

---

## 📅 프로젝트 단계별 역할 분담

### Phase 1: 인프라 구축 (4주)

| 주차 | Platform Engineer | Infrastructure Engineer | Kubernetes Engineer | CI/CD Engineer | Monitoring Engineer | Security Engineer |
|------|-------------------|------------------------|---------------------|----------------|-------------------|------------------|
| **1주** | 아키텍처 설계<br>요구사항 분석 | VPC/네트워킹 구축<br>Terraform 모듈 개발 | EKS 클러스터 설계<br>노드 그룹 계획 | CI/CD 파이프라인 설계<br>GitHub Actions 준비 | 모니터링 아키텍처 설계<br>도구 선정 | 보안 정책 수립<br>IAM 설계 |
| **2주** | 팀 조율<br>진행 상황 점검 | RDS/ElastiCache 구축<br>보안 그룹 설정 | EKS 클러스터 구축<br>Add-ons 설치 | ECR 설정<br>빌드 파이프라인 구축 | Prometheus 클러스터 구축<br>Grafana 설정 | Security Groups 검토<br>암호화 설정 |
| **3주** | 성능 요구사항 검토<br>비용 최적화 계획 | Kafka 클러스터 구축<br>백업 설정 | Kubernetes 매니페스트 작성<br>Helm Chart 개발 | ArgoCD 설치<br>GitOps 설정 | 로깅 시스템 구축<br>알림 설정 | 취약점 스캔 설정<br>컴플라이언스 체크 |
| **4주** | 전체 시스템 검토<br>문서화 | 인프라 테스트<br>재해 복구 테스트 | 서비스 배포 테스트<br>스케일링 테스트 | 배포 파이프라인 테스트<br>롤백 테스트 | 모니터링 대시보드 완성<br>알림 테스트 | 보안 테스트<br>침투 테스트 |

### Phase 2: 서비스 배포 (2주)

| 역할 | 1주차 | 2주차 |
|------|-------|-------|
| **Platform Engineer** | 배포 전략 수립<br>팀 간 조율 | 성능 모니터링<br>최적화 |
| **Infrastructure Engineer** | 인프라 최종 점검<br>성능 튜닝 | 운영 모니터링<br>이슈 대응 |
| **Kubernetes Engineer** | 서비스 배포<br>설정 최적화 | 스케일링 모니터링<br>안정성 확보 |
| **CI/CD Engineer** | 배포 자동화<br>파이프라인 최적화 | 배포 모니터링<br>프로세스 개선 |
| **Monitoring Engineer** | 실시간 모니터링<br>알림 최적화 | SLI/SLO 측정<br>대시보드 개선 |
| **Security Engineer** | 보안 모니터링<br>접근 제어 검증 | 보안 이벤트 분석<br>정책 개선 |

### Phase 3: 운영 및 최적화 (지속적)

**일일 업무**:
- **Platform Engineer**: 전체 시스템 상태 점검, 팀 미팅 주도
- **Infrastructure Engineer**: 인프라 모니터링, 비용 분석, 백업 확인
- **Kubernetes Engineer**: 클러스터 상태 점검, 워크로드 최적화
- **CI/CD Engineer**: 배포 모니터링, 파이프라인 개선
- **Monitoring Engineer**: 알림 분석, 대시보드 업데이트
- **Security Engineer**: 보안 이벤트 분석, 취약점 관리

**주간 업무**:
- 팀 회고 및 개선사항 논의
- 성능 리포트 작성
- 비용 최적화 검토
- 보안 감사
- 문서 업데이트

**월간 업무**:
- 아키텍처 리뷰
- 재해 복구 훈련
- 보안 평가
- 기술 부채 정리
- 교육 및 스킬 개발

---

## 🛠️ 팀별 주요 도구 및 기술 스택

### 공통 도구
```yaml
Communication:
  - Slack (팀 커뮤니케이션)
  - Jira (이슈 트래킹)
  - Confluence (문서화)
  - GitHub (코드 관리)

Monitoring:
  - Grafana (시각화)
  - PagerDuty (알림)
  - Datadog/New Relic (APM)
```

### 역할별 전문 도구

**Platform Engineer**:
- AWS Well-Architected Tool
- Cost Explorer
- Trusted Advisor
- Architecture Decision Records (ADR)

**Infrastructure Engineer**:
- Terraform/Terragrunt
- AWS CLI/CDK
- Ansible/Chef
- Packer

**Kubernetes Engineer**:
- kubectl/kubectx
- Helm/Kustomize
- Lens/K9s
- ArgoCD

**CI/CD Engineer**:
- GitHub Actions
- Docker/Buildx
- Trivy/Snyk
- SonarQube

**Monitoring Engineer**:
- Prometheus/Grafana
- ELK Stack
- Jaeger
- Chaos Engineering Tools

**Security Engineer**:
- AWS Security Hub
- Aqua Security
- Falco
- OWASP Tools

---

## 📈 성공 지표 (KPI)

### 팀 전체 KPI
```yaml
Reliability:
  - 시스템 가용성: 99.9% 이상
  - MTTR (평균 복구 시간): < 30분
  - MTBF (평균 장애 간격): > 30일

Performance:
  - 배포 빈도: 주 5회 이상
  - 배포 성공률: > 95%
  - 리드 타임: < 2시간

Cost:
  - 월 인프라 비용: 예산 대비 ±10% 이내
  - 비용 최적화: 분기별 5% 절감

Security:
  - 보안 사고: 0건
  - 취약점 해결: 7일 이내
  - 컴플라이언스: 100% 준수
```

### 역할별 KPI

**Platform Engineer**:
- 아키텍처 결정 문서화율: 100%
- 팀 만족도: 4.5/5.0 이상
- 비용 최적화 달성률: 분기별 목표 달성

**Infrastructure Engineer**:
- 인프라 가동률: 99.9% 이상
- 백업 성공률: 100%
- 보안 컴플라이언스: 100% 준수

**Kubernetes Engineer**:
- 클러스터 가용성: 99.9% 이상
- 배포 성공률: 95% 이상
- 리소스 사용률 최적화: CPU/Memory 70-80%

**CI/CD Engineer**:
- 파이프라인 성공률: 95% 이상
- 빌드 시간: < 10분
- 배포 시간: < 5분

**Monitoring Engineer**:
- 알림 정확도: 95% 이상 (False Positive < 5%)
- 대시보드 활용도: 일일 접속 100% (팀원)
- 장애 사전 감지율: 80% 이상

**Security Engineer**:
- 취약점 해결 시간: 평균 3일 이내
- 보안 교육 이수율: 100%
- 보안 감사 통과율: 100%

---

## 🎓 교육 및 스킬 개발 계획

### 공통 교육
- AWS 인증 (Solutions Architect, DevOps Engineer)
- Kubernetes 인증 (CKA, CKAD, CKS)
- 보안 교육 (AWS Security, OWASP)
- SRE 방법론 교육

### 역할별 전문 교육

**Platform Engineer**:
- AWS Solutions Architect Professional
- 아키텍처 패턴 및 설계 원칙
- 리더십 및 커뮤니케이션

**Infrastructure Engineer**:
- Terraform Associate/Professional
- AWS Advanced Networking
- 데이터베이스 관리 (PostgreSQL, Redis)

**Kubernetes Engineer**:
- CKA/CKAD/CKS 인증
- Helm Chart 개발
- Service Mesh (Istio) 준비

**CI/CD Engineer**:
- GitHub Actions 고급 과정
- GitOps 패턴 및 ArgoCD
- 컨테이너 보안

**Monitoring Engineer**:
- Prometheus/Grafana 전문 과정
- SRE 방법론 (Google SRE)
- 데이터 분석 및 시각화

**Security Engineer**:
- AWS Security Specialty
- 컨테이너 보안 (CKS)
- 컴플라이언스 프레임워크

---

## 📞 온콜 및 장애 대응

### 온콜 로테이션
```yaml
Primary On-call:
  - Platform Engineer (주중)
  - Infrastructure Engineer (주말)

Secondary On-call:
  - Kubernetes Engineer
  - Monitoring Engineer

Escalation:
  - Level 1: Primary On-call (15분)
  - Level 2: Secondary On-call (30분)
  - Level 3: 전체 팀 (1시간)
```

### 장애 대응 프로세스
1. **감지** (Monitoring Engineer 주도)
2. **분류** (Platform Engineer 판단)
3. **대응** (해당 영역 전문가)
4. **복구** (팀 협력)
5. **포스트모템** (Platform Engineer 주도)

---

이러한 역할 분담을 통해 Popcorn MSA 프로젝트를 안정적이고 효율적으로 운영할 수 있으며, 각 팀원의 전문성을 최대한 활용하면서도 협업을 통한 시너지 효과를 창출할 수 있습니다.