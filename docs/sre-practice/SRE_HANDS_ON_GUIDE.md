# SRE 실무 경험 실습 가이드

## 🎯 실습 개요

이 가이드는 Popcorn MSA 프로젝트를 활용하여 **실제 SRE 업무를 경험**할 수 있도록 구성된 실습 과정입니다. 6개월간의 체계적인 실습을 통해 SRE 전문가로 성장할 수 있습니다.

---

## 📅 6개월 실습 일정

### Month 1-2: SRE 기초 및 환경 구축
- Week 1-2: 이론 학습 및 도구 설치
- Week 3-4: 모니터링 스택 구축
- Week 5-6: 기본 SLI/SLO 정의
- Week 7-8: 알림 시스템 구축

### Month 3-4: 자동화 및 장애 대응
- Week 9-10: 자동화 스크립트 개발
- Week 11-12: 인시던트 대응 프로세스
- Week 13-14: 카오스 엔지니어링
- Week 15-16: 포스트모템 문화 구축

### Month 5-6: 고급 SRE 실무
- Week 17-18: 용량 계획 및 성능 최적화
- Week 19-20: 보안 및 컴플라이언스
- Week 21-22: 플랫폼 엔지니어링
- Week 23-24: 포트폴리오 완성 및 취업 준비

---

## 🛠️ Week 1-2: 환경 구축 및 기초 설정

### 목표
- SRE 실습 환경 구축
- 기본 모니터링 도구 설치
- Popcorn MSA 배포

### 실습 과제

#### 과제 1: AWS 환경 구축
```bash
# 1. AWS CLI 설정
aws configure

# 2. Terraform으로 인프라 구축
cd /Users/beom/IdeaProjects/popcorn-terraform-feature
terraform init
terraform plan -var-file="envs/dev/terraform.tfvars"
terraform apply

# 3. EKS 클러스터 접근 설정
aws eks update-kubeconfig --region ap-northeast-2 --name goorm-popcorn-dev-eks
```

#### 과제 2: 모니터링 스택 설치
```bash
#!/bin/bash
# setup-monitoring.sh

echo "📊 SRE 모니터링 스택 설치"

# Prometheus & Grafana 설치
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --set grafana.adminPassword=sre-admin123 \
  --set prometheus.prometheusSpec.retention=30d \
  --set prometheus.prometheusSpec.storageSpec.volumeClaimTemplate.spec.resources.requests.storage=50Gi

# Jaeger 설치 (분산 추적)
kubectl create namespace observability
kubectl apply -f https://github.com/jaegertracing/jaeger-operator/releases/download/v1.41.0/jaeger-operator.yaml -n observability

# Jaeger 인스턴스 생성
cat <<EOF | kubectl apply -f -
apiVersion: jaegertracing.io/v1
kind: Jaeger
metadata:
  name: jaeger
  namespace: observability
spec:
  strategy: production
  storage:
    type: elasticsearch
    elasticsearch:
      nodeCount: 1
      resources:
        requests:
          memory: "2Gi"
          cpu: "500m"
        limits:
          memory: "2Gi"
          cpu: "500m"
EOF

echo "✅ 모니터링 스택 설치 완료"
```

#### 과제 3: 서비스 배포 및 계측
```yaml
# popcorn-msa-values.yaml
global:
  monitoring:
    enabled: true
    prometheus:
      enabled: true
    jaeger:
      enabled: true

services:
  gateway:
    replicas: 2
    resources:
      requests:
        cpu: 250m
        memory: 256Mi
      limits:
        cpu: 500m
        memory: 512Mi
    monitoring:
      metrics:
        enabled: true
        path: /actuator/prometheus
      tracing:
        enabled: true
        jaegerEndpoint: "http://jaeger-collector.observability:14268/api/traces"

  order:
    replicas: 3
    resources:
      requests:
        cpu: 500m
        memory: 512Mi
      limits:
        cpu: 1000m
        memory: 1Gi
    monitoring:
      metrics:
        enabled: true
      tracing:
        enabled: true
```

### 학습 체크리스트
- [ ] AWS 기본 서비스 이해 (EKS, RDS, ElastiCache)
- [ ] Kubernetes 기본 개념 (Pod, Service, Deployment)
- [ ] Prometheus 메트릭 수집 원리
- [ ] Grafana 대시보드 기본 사용법
- [ ] Jaeger 분산 추적 개념

---

## 📈 Week 3-4: SLI/SLO 정의 및 구현

### 목표
- 서비스별 SLI/SLO 정의
- Error Budget 계산
- SLO 위반 알림 설정

### 실습 과제

#### 과제 1: SLI/SLO 정의서 작성
```yaml
# sli-slo-definitions.yaml
services:
  gateway-service:
    description: "API Gateway - 모든 외부 요청의 진입점"
    business_criticality: "P1"
    slis:
      availability:
        description: "서비스 가용성"
        measurement: "HTTP 200-299 응답 비율"
        prometheus_query: |
          (
            sum(rate(http_requests_total{job="gateway-service",code=~"2.."}[5m])) /
            sum(rate(http_requests_total{job="gateway-service"}[5m]))
          ) * 100
      latency:
        description: "응답 시간"
        measurement: "95% 요청의 응답 시간"
        prometheus_query: |
          histogram_quantile(0.95, 
            sum(rate(http_request_duration_seconds_bucket{job="gateway-service"}[5m])) by (le)
          ) * 1000
      error_rate:
        description: "에러율"
        measurement: "5xx 에러 비율"
        prometheus_query: |
          (
            sum(rate(http_requests_total{job="gateway-service",code=~"5.."}[5m])) /
            sum(rate(http_requests_total{job="gateway-service"}[5m]))
          ) * 100
    slos:
      availability: 99.9%    # 월 43분 다운타임 허용
      latency_p95: 500       # 95% 요청이 500ms 이내
      error_rate: 0.1        # 에러율 0.1% 이하
    error_budget:
      monthly_downtime: "43.2 minutes"
      monthly_error_requests: "0.1% of total requests"

  order-service:
    description: "주문 처리 서비스 - 비즈니스 크리티컬"
    business_criticality: "P0"
    slis:
      availability:
        prometheus_query: |
          (
            sum(rate(http_requests_total{job="order-service",code=~"2.."}[5m])) /
            sum(rate(http_requests_total{job="order-service"}[5m]))
          ) * 100
      latency:
        prometheus_query: |
          histogram_quantile(0.95, 
            sum(rate(http_request_duration_seconds_bucket{job="order-service"}[5m])) by (le)
          ) * 1000
      error_rate:
        prometheus_query: |
          (
            sum(rate(http_requests_total{job="order-service",code=~"5.."}[5m])) /
            sum(rate(http_requests_total{job="order-service"}[5m]))
          ) * 100
    slos:
      availability: 99.95%   # 월 21.6분 다운타임 허용
      latency_p95: 300       # 더 엄격한 응답시간
      error_rate: 0.05       # 더 낮은 에러율
    error_budget:
      monthly_downtime: "21.6 minutes"
      monthly_error_requests: "0.05% of total requests"

  payment-service:
    description: "결제 처리 서비스 - 최고 수준 안정성 요구"
    business_criticality: "P0"
    slis:
      availability:
        prometheus_query: |
          (
            sum(rate(http_requests_total{job="payment-service",code=~"2.."}[5m])) /
            sum(rate(http_requests_total{job="payment-service"}[5m]))
          ) * 100
      latency:
        prometheus_query: |
          histogram_quantile(0.99, 
            sum(rate(http_request_duration_seconds_bucket{job="payment-service"}[5m])) by (le)
          ) * 1000
      error_rate:
        prometheus_query: |
          (
            sum(rate(http_requests_total{job="payment-service",code=~"5.."}[5m])) /
            sum(rate(http_requests_total{job="payment-service"}[5m]))
          ) * 100
    slos:
      availability: 99.99%   # 월 4.32분 다운타임 허용
      latency_p99: 1000      # 99% 요청이 1초 이내
      error_rate: 0.01       # 매우 낮은 에러율
    error_budget:
      monthly_downtime: "4.32 minutes"
      monthly_error_requests: "0.01% of total requests"
```

#### 과제 2: Error Budget 추적 대시보드 생성
```json
{
  "dashboard": {
    "title": "SRE - Error Budget Tracking",
    "panels": [
      {
        "title": "Gateway Service - Error Budget Burn Rate",
        "type": "stat",
        "targets": [
          {
            "expr": "(\n  (\n    sum(rate(http_requests_total{job=\"gateway-service\",code!~\"2..\"}[5m])) /\n    sum(rate(http_requests_total{job=\"gateway-service\"}[5m]))\n  ) * 100\n) / 0.1",
            "legendFormat": "Burn Rate"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "thresholds": {
              "steps": [
                {"color": "green", "value": 0},
                {"color": "yellow", "value": 1},
                {"color": "red", "value": 2}
              ]
            }
          }
        }
      },
      {
        "title": "Order Service - Monthly Error Budget Remaining",
        "type": "gauge",
        "targets": [
          {
            "expr": "100 - (\n  (\n    sum(increase(http_requests_total{job=\"order-service\",code!~\"2..\"}[30d])) /\n    sum(increase(http_requests_total{job=\"order-service\"}[30d]))\n  ) * 100 / 0.05\n)",
            "legendFormat": "Budget Remaining %"
          }
        ]
      },
      {
        "title": "Payment Service - SLO Compliance",
        "type": "table",
        "targets": [
          {
            "expr": "(\n  sum(rate(http_requests_total{job=\"payment-service\",code=~\"2..\"}[5m])) /\n  sum(rate(http_requests_total{job=\"payment-service\"}[5m]))\n) * 100",
            "legendFormat": "Availability %"
          },
          {
            "expr": "histogram_quantile(0.99, \n  sum(rate(http_request_duration_seconds_bucket{job=\"payment-service\"}[5m])) by (le)\n) * 1000",
            "legendFormat": "Latency P99 (ms)"
          }
        ]
      }
    ]
  }
}
```

#### 과제 3: SLO 위반 알림 규칙 생성
```yaml
# slo-alerts.yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: slo-alerts
  namespace: monitoring
spec:
  groups:
  - name: slo.rules
    rules:
    # Gateway Service SLO 알림
    - alert: GatewayServiceAvailabilitySLOViolation
      expr: |
        (
          sum(rate(http_requests_total{job="gateway-service",code=~"2.."}[5m])) /
          sum(rate(http_requests_total{job="gateway-service"}[5m]))
        ) * 100 < 99.9
      for: 2m
      labels:
        severity: critical
        service: gateway-service
        slo_type: availability
      annotations:
        summary: "Gateway Service 가용성 SLO 위반"
        description: "Gateway Service의 가용성이 {{ $value }}%로 SLO 99.9% 미만입니다."
        runbook_url: "https://wiki.company.com/sre/runbooks/gateway-availability"

    - alert: GatewayServiceLatencySLOViolation
      expr: |
        histogram_quantile(0.95, 
          sum(rate(http_request_duration_seconds_bucket{job="gateway-service"}[5m])) by (le)
        ) * 1000 > 500
      for: 5m
      labels:
        severity: warning
        service: gateway-service
        slo_type: latency
      annotations:
        summary: "Gateway Service 응답시간 SLO 위반"
        description: "Gateway Service의 P95 응답시간이 {{ $value }}ms로 SLO 500ms를 초과했습니다."

    # Order Service SLO 알림
    - alert: OrderServiceErrorBudgetBurnRateHigh
      expr: |
        (
          sum(rate(http_requests_total{job="order-service",code!~"2.."}[1h])) /
          sum(rate(http_requests_total{job="order-service"}[1h]))
        ) / 0.0005 > 2  # 2x burn rate
      for: 2m
      labels:
        severity: critical
        service: order-service
        slo_type: error_budget
      annotations:
        summary: "Order Service Error Budget 빠른 소모"
        description: "Order Service의 Error Budget이 정상보다 {{ $value }}배 빠르게 소모되고 있습니다."

    # Payment Service SLO 알림
    - alert: PaymentServiceCriticalSLOViolation
      expr: |
        (
          sum(rate(http_requests_total{job="payment-service",code=~"2.."}[5m])) /
          sum(rate(http_requests_total{job="payment-service"}[5m]))
        ) * 100 < 99.99
      for: 1m
      labels:
        severity: critical
        service: payment-service
        slo_type: availability
        page: "true"
      annotations:
        summary: "Payment Service 크리티컬 SLO 위반"
        description: "Payment Service의 가용성이 {{ $value }}%로 크리티컬 SLO 99.99% 미만입니다. 즉시 대응 필요!"
        runbook_url: "https://wiki.company.com/sre/runbooks/payment-critical"
```

### 학습 체크리스트
- [ ] SLI/SLO/SLA 개념 이해
- [ ] Error Budget 계산 방법
- [ ] Prometheus 쿼리 작성법
- [ ] 비즈니스 임팩트 기반 SLO 설정
- [ ] 알림 피로도 방지 전략

---

## 🚨 Week 5-6: 인시던트 대응 및 자동화

### 목표
- 인시던트 대응 프로세스 구축
- 자동화된 장애 대응 시스템 개발
- 포스트모템 문화 정착

### 실습 과제

#### 과제 1: 인시던트 대응 자동화 시스템
```python
# incident_response_system.py
import asyncio
import aiohttp
import json
from datetime import datetime, timedelta
from typing import Dict, List, Optional
import logging

class IncidentResponseSystem:
    def __init__(self, config: Dict):
        self.config = config
        self.active_incidents = {}
        self.escalation_rules = config.get('escalation_rules', {})
        self.logger = logging.getLogger(__name__)
    
    async def handle_alert(self, alert_data: Dict):
        """알림 처리 및 인시던트 생성"""
        incident_id = self.generate_incident_id(alert_data)
        
        # 중복 인시던트 확인
        if incident_id in self.active_incidents:
            await self.update_existing_incident(incident_id, alert_data)
            return
        
        # 새 인시던트 생성
        incident = {
            'id': incident_id,
            'title': alert_data.get('annotations', {}).get('summary', 'Unknown Alert'),
            'description': alert_data.get('annotations', {}).get('description', ''),
            'severity': alert_data.get('labels', {}).get('severity', 'warning'),
            'service': alert_data.get('labels', {}).get('service', 'unknown'),
            'status': 'open',
            'created_at': datetime.now(),
            'alerts': [alert_data],
            'timeline': [],
            'assignee': None
        }
        
        self.active_incidents[incident_id] = incident
        
        # 자동 대응 실행
        await self.execute_auto_response(incident)
        
        # 알림 전송
        await self.send_notifications(incident)
        
        # 에스컬레이션 스케줄링
        await self.schedule_escalation(incident)
    
    async def execute_auto_response(self, incident: Dict):
        """자동 대응 실행"""
        service = incident['service']
        severity = incident['severity']
        
        self.logger.info(f"Executing auto-response for {service} (severity: {severity})")
        
        # 서비스별 자동 대응 로직
        if service == 'order-service':
            await self.auto_response_order_service(incident)
        elif service == 'payment-service':
            await self.auto_response_payment_service(incident)
        elif service == 'gateway-service':
            await self.auto_response_gateway_service(incident)
    
    async def auto_response_order_service(self, incident: Dict):
        """Order Service 자동 대응"""
        actions_taken = []
        
        try:
            # 1. 현재 상태 확인
            health_status = await self.check_service_health('order-service')
            actions_taken.append(f"Health check: {health_status}")
            
            # 2. 비정상 Pod 재시작
            if health_status.get('unhealthy_pods', 0) > 0:
                restart_result = await self.restart_unhealthy_pods('order-service')
                actions_taken.append(f"Pod restart: {restart_result}")
            
            # 3. 스케일링 (CPU/Memory 사용률 높은 경우)
            metrics = await self.get_service_metrics('order-service')
            if metrics.get('cpu_usage', 0) > 80 or metrics.get('memory_usage', 0) > 80:
                scale_result = await self.scale_service('order-service', target_replicas=5)
                actions_taken.append(f"Auto-scaling: {scale_result}")
            
            # 4. 데이터베이스 연결 확인
            db_status = await self.check_database_connection('order-service')
            actions_taken.append(f"DB connection: {db_status}")
            
            # 타임라인 업데이트
            incident['timeline'].append({
                'timestamp': datetime.now(),
                'action': 'auto_response',
                'details': actions_taken,
                'actor': 'system'
            })
            
        except Exception as e:
            self.logger.error(f"Auto-response failed for order-service: {e}")
            incident['timeline'].append({
                'timestamp': datetime.now(),
                'action': 'auto_response_failed',
                'details': str(e),
                'actor': 'system'
            })
    
    async def auto_response_payment_service(self, incident: Dict):
        """Payment Service 자동 대응 (보수적)"""
        actions_taken = []
        
        try:
            # Payment Service는 보수적 접근
            # 1. 상태 확인만 수행
            health_status = await self.check_service_health('payment-service')
            actions_taken.append(f"Health check: {health_status}")
            
            # 2. 외부 API 연결 확인
            external_api_status = await self.check_external_apis('payment-service')
            actions_taken.append(f"External API status: {external_api_status}")
            
            # 3. 트래픽 패턴 분석
            traffic_analysis = await self.analyze_traffic_pattern('payment-service')
            actions_taken.append(f"Traffic analysis: {traffic_analysis}")
            
            # 자동 재시작이나 스케일링은 하지 않음 (결제 서비스 특성상)
            actions_taken.append("No automatic remediation - manual intervention required")
            
            incident['timeline'].append({
                'timestamp': datetime.now(),
                'action': 'auto_analysis',
                'details': actions_taken,
                'actor': 'system'
            })
            
        except Exception as e:
            self.logger.error(f"Auto-analysis failed for payment-service: {e}")
    
    async def send_notifications(self, incident: Dict):
        """알림 전송"""
        severity = incident['severity']
        service = incident['service']
        
        # Slack 알림
        await self.send_slack_notification(incident)
        
        # PagerDuty (Critical/High severity)
        if severity in ['critical', 'high']:
            await self.create_pagerduty_incident(incident)
        
        # 이메일 (모든 severity)
        await self.send_email_notification(incident)
    
    async def send_slack_notification(self, incident: Dict):
        """Slack 알림 전송"""
        webhook_url = self.config.get('slack_webhook_url')
        if not webhook_url:
            return
        
        severity_emoji = {
            'critical': '🚨',
            'high': '⚠️',
            'warning': '⚡',
            'info': 'ℹ️'
        }
        
        color_map = {
            'critical': '#FF0000',
            'high': '#FF8C00',
            'warning': '#FFD700',
            'info': '#00CED1'
        }
        
        message = {
            "attachments": [
                {
                    "color": color_map.get(incident['severity'], '#808080'),
                    "title": f"{severity_emoji.get(incident['severity'], '🔔')} {incident['title']}",
                    "fields": [
                        {
                            "title": "Service",
                            "value": incident['service'],
                            "short": True
                        },
                        {
                            "title": "Severity",
                            "value": incident['severity'].upper(),
                            "short": True
                        },
                        {
                            "title": "Incident ID",
                            "value": incident['id'],
                            "short": True
                        },
                        {
                            "title": "Created",
                            "value": incident['created_at'].strftime('%Y-%m-%d %H:%M:%S'),
                            "short": True
                        }
                    ],
                    "text": incident['description'],
                    "footer": "SRE Incident Response System",
                    "ts": int(incident['created_at'].timestamp())
                }
            ]
        }
        
        try:
            async with aiohttp.ClientSession() as session:
                async with session.post(webhook_url, json=message) as response:
                    if response.status == 200:
                        self.logger.info(f"Slack notification sent for incident {incident['id']}")
                    else:
                        self.logger.error(f"Failed to send Slack notification: {response.status}")
        except Exception as e:
            self.logger.error(f"Slack notification error: {e}")
    
    async def check_service_health(self, service_name: str) -> Dict:
        """서비스 헬스 체크"""
        try:
            # Kubernetes API를 통한 Pod 상태 확인
            # 실제 구현에서는 kubernetes 라이브러리 사용
            cmd = f"kubectl get pods -n popcorn -l app={service_name} -o json"
            # subprocess 또는 kubernetes client 사용
            
            # 시뮬레이션 데이터
            return {
                'total_pods': 3,
                'healthy_pods': 2,
                'unhealthy_pods': 1,
                'status': 'degraded'
            }
        except Exception as e:
            return {'error': str(e), 'status': 'unknown'}
    
    async def get_service_metrics(self, service_name: str) -> Dict:
        """서비스 메트릭 조회"""
        try:
            prometheus_url = self.config.get('prometheus_url', 'http://prometheus:9090')
            
            queries = {
                'cpu_usage': f'avg(rate(container_cpu_usage_seconds_total{{pod=~"{service_name}-.*"}}[5m])) * 100',
                'memory_usage': f'avg(container_memory_working_set_bytes{{pod=~"{service_name}-.*"}}) / avg(container_spec_memory_limit_bytes{{pod=~"{service_name}-.*"}}) * 100',
                'request_rate': f'sum(rate(http_requests_total{{job="{service_name}"}}[5m]))',
                'error_rate': f'sum(rate(http_requests_total{{job="{service_name}",code=~"5.."}}[5m])) / sum(rate(http_requests_total{{job="{service_name}"}}[5m])) * 100'
            }
            
            metrics = {}
            async with aiohttp.ClientSession() as session:
                for metric_name, query in queries.items():
                    try:
                        async with session.get(f"{prometheus_url}/api/v1/query", 
                                             params={'query': query}) as response:
                            if response.status == 200:
                                data = await response.json()
                                if data['data']['result']:
                                    metrics[metric_name] = float(data['data']['result'][0]['value'][1])
                                else:
                                    metrics[metric_name] = 0
                    except Exception as e:
                        self.logger.error(f"Failed to get {metric_name}: {e}")
                        metrics[metric_name] = 0
            
            return metrics
        except Exception as e:
            return {'error': str(e)}
    
    def generate_incident_id(self, alert_data: Dict) -> str:
        """인시던트 ID 생성"""
        service = alert_data.get('labels', {}).get('service', 'unknown')
        alert_name = alert_data.get('labels', {}).get('alertname', 'unknown')
        timestamp = datetime.now().strftime('%Y%m%d%H%M')
        return f"INC-{service.upper()}-{alert_name}-{timestamp}"

# 사용 예시
async def main():
    config = {
        'slack_webhook_url': 'https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK',
        'pagerduty_api_key': 'YOUR_PAGERDUTY_API_KEY',
        'prometheus_url': 'http://prometheus:9090',
        'escalation_rules': {
            'critical': {'initial_timeout': 300, 'escalation_timeout': 900},
            'high': {'initial_timeout': 600, 'escalation_timeout': 1800}
        }
    }
    
    incident_system = IncidentResponseSystem(config)
    
    # 테스트 알림 데이터
    test_alert = {
        'labels': {
            'alertname': 'OrderServiceHighErrorRate',
            'service': 'order-service',
            'severity': 'critical'
        },
        'annotations': {
            'summary': 'Order Service 에러율 급증',
            'description': 'Order Service의 5xx 에러율이 5% 이상으로 증가했습니다.'
        }
    }
    
    await incident_system.handle_alert(test_alert)

if __name__ == "__main__":
    asyncio.run(main())
```

### 학습 체크리스트
- [ ] 인시던트 대응 프로세스 이해
- [ ] 자동화 vs 수동 대응 판단 기준
- [ ] 알림 피로도 방지 전략
- [ ] 에스컬레이션 정책 설계
- [ ] 포스트모템 작성법

---

## 🔬 Week 7-8: 카오스 엔지니어링 실습

### 목표
- 시스템 복원력 테스트
- 장애 시나리오 설계 및 실행
- 복구 능력 검증

### 실습 과제

#### 과제 1: 카오스 실험 설계
```python
# chaos_experiments.py
import asyncio
import random
import subprocess
import json
from datetime import datetime, timedelta
from typing import List, Dict, Optional

class ChaosExperimentSuite:
    def __init__(self, namespace: str = "popcorn"):
        self.namespace = namespace
        self.experiments = []
        self.baseline_metrics = {}
        self.experiment_results = {}
    
    async def run_experiment_suite(self):
        """전체 카오스 실험 스위트 실행"""
        print("🔬 카오스 엔지니어링 실험 시작")
        print("=" * 50)
        
        # 1. 베이스라인 메트릭 수집
        await self.collect_baseline_metrics()
        
        # 2. 실험 시나리오 실행
        experiments = [
            self.pod_failure_experiment,
            self.network_latency_experiment,
            self.resource_exhaustion_experiment,
            self.database_connection_experiment,
            self.traffic_spike_experiment
        ]
        
        for experiment in experiments:
            try:
                print(f"\n🧪 실험 시작: {experiment.__name__}")
                result = await experiment()
                self.experiment_results[experiment.__name__] = result
                
                # 실험 간 복구 시간
                await asyncio.sleep(120)
                
            except Exception as e:
                print(f"❌ 실험 실패: {experiment.__name__} - {e}")
                self.experiment_results[experiment.__name__] = {'error': str(e)}
        
        # 3. 결과 분석 및 보고서 생성
        await self.generate_experiment_report()
    
    async def collect_baseline_metrics(self):
        """베이스라인 메트릭 수집"""
        print("📊 베이스라인 메트릭 수집 중...")
        
        services = ['gateway-service', 'order-service', 'payment-service', 'user-service']
        
        for service in services:
            metrics = await self.get_service_metrics(service)
            self.baseline_metrics[service] = {
                'timestamp': datetime.now(),
                'metrics': metrics,
                'pod_count': await self.get_pod_count(service)
            }
        
        print("✅ 베이스라인 메트릭 수집 완료")
    
    async def pod_failure_experiment(self) -> Dict:
        """Pod 장애 실험"""
        experiment_name = "Pod Failure Experiment"
        print(f"🔥 {experiment_name} 시작")
        
        # 실험 대상 서비스 선택
        target_service = "order-service"
        
        # 1. 현재 Pod 수 확인
        initial_pod_count = await self.get_pod_count(target_service)
        print(f"초기 Pod 수: {initial_pod_count}")
        
        # 2. 랜덤 Pod 종료
        killed_pods = await self.kill_random_pods(target_service, count=1)
        experiment_start = datetime.now()
        
        # 3. 복구 모니터링
        recovery_time = await self.monitor_service_recovery(target_service, initial_pod_count)
        
        # 4. 메트릭 수집
        post_experiment_metrics = await self.get_service_metrics(target_service)
        
        result = {
            'experiment': experiment_name,
            'target_service': target_service,
            'killed_pods': killed_pods,
            'initial_pod_count': initial_pod_count,
            'recovery_time_seconds': recovery_time,
            'baseline_metrics': self.baseline_metrics.get(target_service, {}),
            'post_experiment_metrics': post_experiment_metrics,
            'success': recovery_time < 300,  # 5분 내 복구
            'timestamp': experiment_start
        }
        
        print(f"✅ Pod 장애 실험 완료 - 복구 시간: {recovery_time}초")
        return result
    
    async def network_latency_experiment(self) -> Dict:
        """네트워크 지연 실험"""
        experiment_name = "Network Latency Experiment"
        print(f"🌐 {experiment_name} 시작")
        
        target_service = "payment-service"
        latency_ms = 500  # 500ms 지연 주입
        
        # 1. 베이스라인 응답시간 측정
        baseline_latency = await self.measure_service_latency(target_service)
        
        # 2. 네트워크 지연 주입 (Istio VirtualService 사용)
        await self.inject_network_latency(target_service, latency_ms)
        experiment_start = datetime.now()
        
        # 3. 지연 시간 동안 메트릭 모니터링
        await asyncio.sleep(300)  # 5분간 모니터링
        
        # 4. 지연된 상태에서 메트릭 수집
        degraded_metrics = await self.get_service_metrics(target_service)
        degraded_latency = await self.measure_service_latency(target_service)
        
        # 5. 지연 제거
        await self.remove_network_latency(target_service)
        
        # 6. 복구 확인
        await asyncio.sleep(60)  # 1분 대기
        recovered_latency = await self.measure_service_latency(target_service)
        
        result = {
            'experiment': experiment_name,
            'target_service': target_service,
            'injected_latency_ms': latency_ms,
            'baseline_latency_ms': baseline_latency,
            'degraded_latency_ms': degraded_latency,
            'recovered_latency_ms': recovered_latency,
            'degraded_metrics': degraded_metrics,
            'latency_increase': degraded_latency - baseline_latency,
            'recovery_successful': abs(recovered_latency - baseline_latency) < 50,
            'timestamp': experiment_start
        }
        
        print(f"✅ 네트워크 지연 실험 완료")
        return result
    
    async def resource_exhaustion_experiment(self) -> Dict:
        """리소스 고갈 실험"""
        experiment_name = "Resource Exhaustion Experiment"
        print(f"💻 {experiment_name} 시작")
        
        target_service = "user-service"
        
        # 1. 베이스라인 리소스 사용률
        baseline_metrics = await self.get_service_metrics(target_service)
        
        # 2. CPU 스트레스 Pod 생성
        stress_pod_name = await self.create_cpu_stress_pod(target_service)
        experiment_start = datetime.now()
        
        # 3. 스트레스 테스트 중 메트릭 모니터링
        stress_duration = 300  # 5분
        monitoring_interval = 30  # 30초마다 체크
        
        stress_metrics = []
        for i in range(stress_duration // monitoring_interval):
            await asyncio.sleep(monitoring_interval)
            current_metrics = await self.get_service_metrics(target_service)
            stress_metrics.append({
                'timestamp': datetime.now(),
                'metrics': current_metrics
            })
            
            # HPA 동작 확인
            current_pod_count = await self.get_pod_count(target_service)
            print(f"현재 Pod 수: {current_pod_count}, CPU: {current_metrics.get('cpu_usage', 0):.1f}%")
        
        # 4. 스트레스 Pod 제거
        await self.delete_stress_pod(stress_pod_name)
        
        # 5. 복구 모니터링
        await asyncio.sleep(120)  # 2분 대기
        recovered_metrics = await self.get_service_metrics(target_service)
        final_pod_count = await self.get_pod_count(target_service)
        
        result = {
            'experiment': experiment_name,
            'target_service': target_service,
            'baseline_metrics': baseline_metrics,
            'stress_metrics': stress_metrics,
            'recovered_metrics': recovered_metrics,
            'max_cpu_usage': max([m['metrics'].get('cpu_usage', 0) for m in stress_metrics]),
            'hpa_triggered': final_pod_count > self.baseline_metrics[target_service]['pod_count'],
            'final_pod_count': final_pod_count,
            'timestamp': experiment_start
        }
        
        print(f"✅ 리소스 고갈 실험 완료")
        return result
    
    async def database_connection_experiment(self) -> Dict:
        """데이터베이스 연결 실험"""
        experiment_name = "Database Connection Experiment"
        print(f"🗄️ {experiment_name} 시작")
        
        # 데이터베이스 연결 풀 고갈 시뮬레이션
        target_service = "order-service"
        
        # 1. 베이스라인 DB 메트릭
        baseline_db_metrics = await self.get_database_metrics()
        
        # 2. 연결 풀 스트레스 테스트
        await self.create_db_connection_stress()
        experiment_start = datetime.now()
        
        # 3. 모니터링
        await asyncio.sleep(180)  # 3분간 모니터링
        
        # 4. 스트레스 중 메트릭
        stress_db_metrics = await self.get_database_metrics()
        service_metrics = await self.get_service_metrics(target_service)
        
        # 5. 스트레스 제거
        await self.remove_db_connection_stress()
        
        # 6. 복구 확인
        await asyncio.sleep(60)
        recovered_db_metrics = await self.get_database_metrics()
        
        result = {
            'experiment': experiment_name,
            'target_service': target_service,
            'baseline_db_connections': baseline_db_metrics.get('active_connections', 0),
            'stress_db_connections': stress_db_metrics.get('active_connections', 0),
            'recovered_db_connections': recovered_db_metrics.get('active_connections', 0),
            'service_error_rate': service_metrics.get('error_rate', 0),
            'connection_pool_exhausted': stress_db_metrics.get('active_connections', 0) > 80,
            'timestamp': experiment_start
        }
        
        print(f"✅ 데이터베이스 연결 실험 완료")
        return result
    
    async def traffic_spike_experiment(self) -> Dict:
        """트래픽 급증 실험"""
        experiment_name = "Traffic Spike Experiment"
        print(f"📈 {experiment_name} 시작")
        
        target_service = "gateway-service"
        
        # 1. 베이스라인 트래픽
        baseline_rps = await self.measure_request_rate(target_service)
        
        # 2. 부하 테스트 실행
        load_test_pod = await self.create_load_test_pod(target_service, target_rps=1000)
        experiment_start = datetime.now()
        
        # 3. 부하 테스트 중 모니터링
        load_test_duration = 600  # 10분
        monitoring_data = []
        
        for i in range(load_test_duration // 30):  # 30초마다 체크
            await asyncio.sleep(30)
            current_metrics = await self.get_service_metrics(target_service)
            current_rps = await self.measure_request_rate(target_service)
            pod_count = await self.get_pod_count(target_service)
            
            monitoring_data.append({
                'timestamp': datetime.now(),
                'rps': current_rps,
                'pod_count': pod_count,
                'cpu_usage': current_metrics.get('cpu_usage', 0),
                'memory_usage': current_metrics.get('memory_usage', 0),
                'error_rate': current_metrics.get('error_rate', 0),
                'latency_p95': current_metrics.get('latency_p95', 0)
            })
            
            print(f"RPS: {current_rps}, Pods: {pod_count}, CPU: {current_metrics.get('cpu_usage', 0):.1f}%")
        
        # 4. 부하 테스트 중단
        await self.delete_load_test_pod(load_test_pod)
        
        # 5. 복구 모니터링
        await asyncio.sleep(300)  # 5분 대기
        recovered_metrics = await self.get_service_metrics(target_service)
        final_pod_count = await self.get_pod_count(target_service)
        
        result = {
            'experiment': experiment_name,
            'target_service': target_service,
            'baseline_rps': baseline_rps,
            'target_rps': 1000,
            'monitoring_data': monitoring_data,
            'max_rps_achieved': max([d['rps'] for d in monitoring_data]),
            'max_pod_count': max([d['pod_count'] for d in monitoring_data]),
            'max_error_rate': max([d['error_rate'] for d in monitoring_data]),
            'hpa_effective': max([d['pod_count'] for d in monitoring_data]) > self.baseline_metrics[target_service]['pod_count'],
            'final_pod_count': final_pod_count,
            'timestamp': experiment_start
        }
        
        print(f"✅ 트래픽 급증 실험 완료")
        return result
    
    async def generate_experiment_report(self):
        """실험 결과 보고서 생성"""
        print("\n📋 카오스 엔지니어링 실험 보고서")
        print("=" * 60)
        
        report = {
            'experiment_suite': 'Popcorn MSA Chaos Engineering',
            'execution_date': datetime.now(),
            'baseline_metrics': self.baseline_metrics,
            'experiments': self.experiment_results,
            'summary': {
                'total_experiments': len(self.experiment_results),
                'successful_experiments': 0,
                'failed_experiments': 0,
                'system_resilience_score': 0
            },
            'recommendations': []
        }
        
        # 결과 분석
        for exp_name, result in self.experiment_results.items():
            if result.get('error'):
                report['summary']['failed_experiments'] += 1
                print(f"❌ {exp_name}: FAILED - {result['error']}")
            else:
                success = result.get('success', True)
                if success:
                    report['summary']['successful_experiments'] += 1
                    print(f"✅ {exp_name}: PASSED")
                else:
                    report['summary']['failed_experiments'] += 1
                    print(f"⚠️ {exp_name}: FAILED")
        
        # 복원력 점수 계산
        total_experiments = report['summary']['total_experiments']
        successful_experiments = report['summary']['successful_experiments']
        
        if total_experiments > 0:
            resilience_score = (successful_experiments / total_experiments) * 100
            report['summary']['system_resilience_score'] = resilience_score
            print(f"\n🎯 시스템 복원력 점수: {resilience_score:.1f}%")
        
        # 권장사항 생성
        recommendations = self.generate_recommendations(report)
        report['recommendations'] = recommendations
        
        print(f"\n📝 권장사항:")
        for i, rec in enumerate(recommendations, 1):
            print(f"{i}. {rec}")
        
        # 보고서 저장
        with open(f'chaos_experiment_report_{datetime.now().strftime("%Y%m%d_%H%M%S")}.json', 'w') as f:
            json.dump(report, f, indent=2, default=str)
        
        print(f"\n💾 보고서 저장 완료")
    
    def generate_recommendations(self, report: Dict) -> List[str]:
        """실험 결과 기반 권장사항 생성"""
        recommendations = []
        
        # 복원력 점수 기반 권장사항
        resilience_score = report['summary']['system_resilience_score']
        
        if resilience_score < 70:
            recommendations.append("시스템 복원력이 낮습니다. 자동 복구 메커니즘 강화가 필요합니다.")
        
        # 실험별 권장사항
        for exp_name, result in report['experiments'].items():
            if 'pod_failure' in exp_name.lower():
                if result.get('recovery_time_seconds', 0) > 180:
                    recommendations.append("Pod 장애 복구 시간이 깁니다. Health Check 및 Readiness Probe 최적화를 권장합니다.")
            
            elif 'network_latency' in exp_name.lower():
                if result.get('latency_increase', 0) > 1000:
                    recommendations.append("네트워크 지연에 대한 내성이 부족합니다. Circuit Breaker 패턴 도입을 권장합니다.")
            
            elif 'resource_exhaustion' in exp_name.lower():
                if not result.get('hpa_triggered', False):
                    recommendations.append("HPA가 제대로 동작하지 않습니다. 스케일링 정책 검토가 필요합니다.")
            
            elif 'traffic_spike' in exp_name.lower():
                max_error_rate = result.get('max_error_rate', 0)
                if max_error_rate > 1:
                    recommendations.append("트래픽 급증 시 에러율이 높습니다. 로드밸런싱 및 백프레셔 메커니즘 검토가 필요합니다.")
        
        if not recommendations:
            recommendations.append("모든 실험이 성공적으로 완료되었습니다. 현재 시스템의 복원력이 우수합니다.")
        
        return recommendations
    
    # 헬퍼 메서드들 (실제 구현에서는 Kubernetes API 사용)
    async def get_pod_count(self, service_name: str) -> int:
        """Pod 수 조회"""
        # kubectl get pods -n popcorn -l app=service_name --no-headers | wc -l
        return random.randint(2, 5)  # 시뮬레이션
    
    async def kill_random_pods(self, service_name: str, count: int = 1) -> List[str]:
        """랜덤 Pod 종료"""
        # kubectl delete pod -n popcorn -l app=service_name --random
        return [f"{service_name}-pod-{i}" for i in range(count)]  # 시뮬레이션
    
    async def get_service_metrics(self, service_name: str) -> Dict:
        """서비스 메트릭 조회"""
        # Prometheus 쿼리 실행
        return {
            'cpu_usage': random.uniform(20, 80),
            'memory_usage': random.uniform(30, 70),
            'request_rate': random.uniform(50, 200),
            'error_rate': random.uniform(0, 2),
            'latency_p95': random.uniform(100, 500)
        }  # 시뮬레이션

# 사용 예시
async def main():
    chaos_suite = ChaosExperimentSuite()
    await chaos_suite.run_experiment_suite()

if __name__ == "__main__":
    asyncio.run(main())
```

### 학습 체크리스트
- [ ] 카오스 엔지니어링 원칙 이해
- [ ] 실험 설계 및 가설 수립
- [ ] 안전한 실험 환경 구축
- [ ] 복원력 메트릭 정의
- [ ] 실험 결과 분석 및 개선

---

## 🎓 최종 프로젝트: SRE 포트폴리오 완성

### 목표
- 종합적인 SRE 시스템 구축
- 실무 경험 포트폴리오 완성
- 취업 준비 및 면접 대비

### 포트폴리오 구성 요소

1. **SRE 대시보드** - Grafana 기반 종합 모니터링
2. **자동화 시스템** - Python 기반 인시던트 대응
3. **카오스 엔지니어링** - 시스템 복원력 테스트
4. **용량 계획** - 데이터 기반 확장 계획
5. **문서화** - 운영 가이드 및 포스트모템

### 성과 측정 지표

```yaml
기술적 성과:
  - 시스템 가용성: 99.9% 이상 달성
  - MTTR: 5분 이내
  - 자동화율: 80% 이상
  - SLO 준수율: 95% 이상

학습 성과:
  - 완료한 실습 과제: 24개
  - 작성한 코드 라인: 5,000+ 라인
  - 생성한 문서: 20+ 페이지
  - 해결한 인시던트: 10+ 건

포트폴리오 완성도:
  - GitHub 저장소 정리
  - 기술 블로그 포스팅
  - 발표 자료 준비
  - 면접 준비 완료
```

이 실습 가이드를 통해 6개월간 체계적으로 SRE 실무 경험을 쌓으면, 실제 SRE 포지션에 지원할 때 강력한 포트폴리오를 갖출 수 있습니다. 특히 현재 Popcorn MSA 프로젝트를 활용하면 실제 서비스 운영 경험을 쌓을 수 있어 매우 유리합니다.