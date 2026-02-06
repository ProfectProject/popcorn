# Monitoring & Observability Engineer 상세 실무 가이드

## 🎯 역할 개요

**Monitoring & Observability Engineer**는 Popcorn MSA 프로젝트의 **시스템 모니터링, 로깅, 알림 시스템을 구축하고 관리**하는 핵심 역할입니다. **Grafana LGTM 스택**(Loki, Grafana, Tempo, Mimir)과 OpenTelemetry를 활용한 완전한 관찰성(Observability) 환경을 구축하여 분산 트레이싱, 메트릭, 로그를 통합 관리합니다.

## 🔧 Grafana LGTM 스택 개요

**LGTM 스택 구성 요소**:
- **L**oki: 로그 집계 및 검색 엔진
- **G**rafana: 통합 시각화 및 대시보드 플랫폼  
- **T**empo: 분산 트레이싱 백엔드 (OpenTelemetry 호환)
- **M**imir: 장기 메트릭 저장소 (Prometheus 호환)

**기술별 상세 역할**:

| 기술 | 주요 역할 | 적용 방식 | 데이터 보존 |
|------|-----------|-----------|-------------|
| **Loki** | 로그 집계/검색/라벨 기반 쿼리 | Promtail을 통한 Pod 로그 수집 | 7일 (운영), 3일 (개발) |
| **Grafana** | 통합 대시보드/알림/시각화 | LGTM 스택 중앙 허브 역할 | 대시보드 설정 영구 저장 |
| **Tempo** | 분산 트레이싱/요청 흐름 추적 | OpenTelemetry Agent 연동 | 7일 (압축 저장) |
| **Mimir** | 메트릭 장기 저장/고가용성 | Prometheus 메트릭 원격 저장 | 30일 (상세), 1년 (집계) |

**LGTM 스택 통합 이점**:
- **단일 인터페이스**: Grafana에서 로그, 메트릭, 트레이스 통합 조회
- **상관관계 분석**: 트레이스 ID로 로그-메트릭-트레이스 연결
- **확장성**: 클라우드 네이티브 아키텍처로 수평 확장
- **비용 효율성**: 라벨 기반 인덱싱으로 스토리지 최적화

---

## 📋 주요 책임 및 업무

### 핵심 책임
- **Grafana LGTM 스택** 구축 및 관리 (Loki, Grafana, Tempo, Mimir)
- **OpenTelemetry** 기반 분산 추적 및 메트릭 수집
- **통합 관찰성** 환경 구축 (로그-메트릭-트레이스 상관관계)
- 로그 집계 및 분석 시스템 구축 (Loki)
- 장기 메트릭 저장 및 분석 (Mimir)
- 분산 트레이싱 시스템 운영 (Tempo)
- 알림 및 온콜 시스템 설정
- SLI/SLO 정의 및 모니터링
- 성능 분석 및 최적화
- MSA 환경 요청 흐름 추적 및 병목 지점 분석

### 일일 업무
- LGTM 스택 상태 모니터링 및 헬스체크
- 시스템 메트릭 모니터링 (Mimir 기반)
- 로그 분석 및 이슈 추적 (Loki 기반)
- 분산 트레이싱 데이터 분석 (Tempo 기반)
- 알림 및 대시보드 최적화
- SLO 준수율 확인
- 성능 병목 지점 분석
- 스토리지 사용량 및 보존 정책 관리

---

## 📊 Week 1: Grafana LGTM 스택 완전 구축

### Day 1-2: LGTM 스택 통합 설치

#### 1.1 LGTM 스택 통합 개요

**Grafana LGTM 스택 아키텍처**:
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Application   │    │   Application   │    │   Application   │
│   (User Service)│    │ (Order Service) │    │(Payment Service)│
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          ▼                      ▼                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                    OpenTelemetry Collector                     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │
│  │   Traces    │  │   Metrics   │  │         Logs            │ │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘ │
└─────────┬───────────────┬─────────────────────┬─────────────────┘
          │               │                     │
          ▼               ▼                     ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│     Tempo       │ │     Mimir       │ │      Loki       │
│ (Distributed    │ │ (Long-term      │ │ (Log Aggregation│
│  Tracing)       │ │  Metrics)       │ │  & Search)      │
└─────────────────┘ └─────────────────┘ └─────────────────┘
          │               │                     │
          └───────────────┼─────────────────────┘
                          ▼
                ┌─────────────────┐
                │     Grafana     │
                │ (Visualization  │
                │ & Dashboards)   │
                └─────────────────┘
```

**LGTM 스택 설치 스크립트**:
```bash
#!/bin/bash
# scripts/install-lgtm-stack.sh

NAMESPACE="monitoring"
RELEASE_NAME="lgtm"

echo "📊 Grafana LGTM 스택 통합 설치"

# Helm 레포지토리 추가
helm repo add grafana https://grafana.github.io/helm-charts
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# 네임스페이스 생성
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# LGTM 스택 통합 설치
helm install $RELEASE_NAME grafana/lgtm-distributed \
  --namespace $NAMESPACE \
  --values lgtm-values.yaml \
  --wait --timeout=30m

# OpenTelemetry Collector 설치
helm install otel-collector prometheus-community/opentelemetry-collector \
  --namespace $NAMESPACE \
  --values otel-collector-values.yaml \
  --wait --timeout=10m

# 설치 확인
echo "🔍 설치 상태 확인"
kubectl get pods -n $NAMESPACE
kubectl get svc -n $NAMESPACE

# 헬스체크
echo "🏥 헬스체크 수행"
kubectl get pods -n $NAMESPACE -l app.kubernetes.io/name=loki
kubectl get pods -n $NAMESPACE -l app.kubernetes.io/name=grafana
kubectl get pods -n $NAMESPACE -l app.kubernetes.io/name=tempo
kubectl get pods -n $NAMESPACE -l app.kubernetes.io/name=mimir

echo "✅ LGTM 스택 설치 완료"
```

#### 1.2 LGTM 스택 설정

**lgtm-values.yaml**:
```yaml
# Global 설정
global:
  image:
    registry: docker.io
  clusterDomain: cluster.local
  dnsService: kube-dns
  dnsNamespace: kube-system

# Loki 설정 (로그 집계)
loki:
  enabled: true
  
  # 분산 모드 설정
  deploymentMode: Distributed
  
  # 스토리지 설정
  storage:
    type: s3
    s3:
      endpoint: s3.ap-northeast-2.amazonaws.com
      region: ap-northeast-2
      bucketNames:
        chunks: popcorn-loki-chunks-${ENVIRONMENT}
        ruler: popcorn-loki-ruler-${ENVIRONMENT}
        admin: popcorn-loki-admin-${ENVIRONMENT}
      accessKeyId: ${AWS_ACCESS_KEY_ID}
      secretAccessKey: ${AWS_SECRET_ACCESS_KEY}
  
  # 컴포넌트별 설정
  ingester:
    replicas: 3
    persistence:
      enabled: true
      storageClass: gp3-encrypted
      size: 50Gi
    resources:
      requests:
        cpu: 500m
        memory: 1Gi
      limits:
        cpu: 1000m
        memory: 2Gi
  
  distributor:
    replicas: 2
    resources:
      requests:
        cpu: 100m
        memory: 256Mi
      limits:
        cpu: 500m
        memory: 512Mi
  
  querier:
    replicas: 2
    resources:
      requests:
        cpu: 500m
        memory: 512Mi
      limits:
        cpu: 1000m
        memory: 1Gi
  
  queryFrontend:
    replicas: 2
    resources:
      requests:
        cpu: 100m
        memory: 256Mi
      limits:
        cpu: 500m
        memory: 512Mi
  
  # 보존 정책
  limits_config:
    retention_period: 168h  # 7일
    ingestion_rate_mb: 16
    ingestion_burst_size_mb: 32

# Grafana 설정
grafana:
  enabled: true
  
  # 관리자 설정
  adminUser: admin
  adminPassword: ${GRAFANA_ADMIN_PASSWORD}
  
  # 영구 스토리지
  persistence:
    enabled: true
    storageClassName: gp3-encrypted
    size: 10Gi
  
  # 리소스 설정
  resources:
    requests:
      cpu: 100m
      memory: 256Mi
    limits:
      cpu: 500m
      memory: 512Mi
  
  # 플러그인 설치
  plugins:
    - grafana-piechart-panel
    - grafana-worldmap-panel
    - grafana-clock-panel
    - grafana-polystat-panel
  
  # 데이터소스 자동 설정
  datasources:
    datasources.yaml:
      apiVersion: 1
      datasources:
      # Mimir (Prometheus 호환)
      - name: Mimir
        type: prometheus
        url: http://mimir-query-frontend:8080/prometheus
        access: proxy
        isDefault: true
        jsonData:
          timeInterval: 30s
      
      # Loki (로그)
      - name: Loki
        type: loki
        url: http://loki-query-frontend:3100
        access: proxy
        jsonData:
          derivedFields:
            - datasourceUid: tempo
              matcherRegex: "traceID=(\\w+)"
              name: TraceID
              url: "$${__value.raw}"
      
      # Tempo (분산 추적)
      - name: Tempo
        type: tempo
        url: http://tempo-query-frontend:3100
        access: proxy
        uid: tempo
        jsonData:
          tracesToLogs:
            datasourceUid: loki
            tags: ['job', 'instance', 'pod', 'namespace']
            mappedTags: [{ key: 'service.name', value: 'service' }]
            mapTagNamesEnabled: false
            spanStartTimeShift: '1h'
            spanEndTimeShift: '1h'
            filterByTraceID: false
            filterBySpanID: false
          tracesToMetrics:
            datasourceUid: mimir
            tags: [{ key: 'service.name', value: 'service' }, { key: 'job' }]
            queries:
              - name: 'Sample query'
                query: 'sum(rate(traces_spanmetrics_latency_bucket{$$__tags}[5m]))'
          serviceMap:
            datasourceUid: mimir
          search:
            hide: false
          nodeGraph:
            enabled: true

# Tempo 설정 (분산 추적)
tempo:
  enabled: true
  
  # 분산 모드 설정
  deploymentMode: Distributed
  
  # 스토리지 설정
  storage:
    trace:
      backend: s3
      s3:
        endpoint: s3.ap-northeast-2.amazonaws.com
        bucket: popcorn-tempo-traces-${ENVIRONMENT}
        region: ap-northeast-2
        access_key: ${AWS_ACCESS_KEY_ID}
        secret_key: ${AWS_SECRET_ACCESS_KEY}
  
  # 컴포넌트별 설정
  ingester:
    replicas: 3
    persistence:
      enabled: true
      storageClass: gp3-encrypted
      size: 50Gi
    resources:
      requests:
        cpu: 500m
        memory: 1Gi
      limits:
        cpu: 1000m
        memory: 2Gi
  
  distributor:
    replicas: 2
    resources:
      requests:
        cpu: 100m
        memory: 256Mi
      limits:
        cpu: 500m
        memory: 512Mi
  
  querier:
    replicas: 2
    resources:
      requests:
        cpu: 500m
        memory: 512Mi
      limits:
        cpu: 1000m
        memory: 1Gi
  
  queryFrontend:
    replicas: 2
    resources:
      requests:
        cpu: 100m
        memory: 256Mi
      limits:
        cpu: 500m
        memory: 512Mi
  
  # 보존 정책
  retention: 168h  # 7일

# Mimir 설정 (장기 메트릭 저장)
mimir:
  enabled: true
  
  # 분산 모드 설정
  deploymentMode: Distributed
  
  # 스토리지 설정
  storage:
    backend: s3
    s3:
      endpoint: s3.ap-northeast-2.amazonaws.com
      bucket_name: popcorn-mimir-blocks-${ENVIRONMENT}
      region: ap-northeast-2
      access_key_id: ${AWS_ACCESS_KEY_ID}
      secret_access_key: ${AWS_SECRET_ACCESS_KEY}
  
  # 컴포넌트별 설정
  ingester:
    replicas: 3
    persistence:
      enabled: true
      storageClass: gp3-encrypted
      size: 50Gi
    resources:
      requests:
        cpu: 500m
        memory: 2Gi
      limits:
        cpu: 1000m
        memory: 4Gi
  
  distributor:
    replicas: 2
    resources:
      requests:
        cpu: 100m
        memory: 256Mi
      limits:
        cpu: 500m
        memory: 512Mi
  
  querier:
    replicas: 2
    resources:
      requests:
        cpu: 500m
        memory: 1Gi
      limits:
        cpu: 1000m
        memory: 2Gi
  
  queryFrontend:
    replicas: 2
    resources:
      requests:
        cpu: 100m
        memory: 256Mi
      limits:
        cpu: 500m
        memory: 512Mi
  
  # 보존 정책
  limits:
    compactor_blocks_retention_period: 30d  # 30일 보존

# Promtail 설정 (로그 수집)
promtail:
  enabled: true
  
  # DaemonSet으로 모든 노드에 배포
  daemonset:
    enabled: true
  
  # 리소스 설정
  resources:
    requests:
      cpu: 100m
      memory: 128Mi
    limits:
      cpu: 200m
      memory: 256Mi
  
  # 설정
  config:
    clients:
      - url: http://loki-distributor:3100/loki/api/v1/push
    
    scrape_configs:
      # Kubernetes Pod 로그 수집
      - job_name: kubernetes-pods
        kubernetes_sd_configs:
          - role: pod
        
        relabel_configs:
          # 네임스페이스 필터링 (popcorn, monitoring만)
          - source_labels: [__meta_kubernetes_namespace]
            regex: (popcorn|monitoring)
            action: keep
          
          # 메타데이터 추출
          - source_labels: [__meta_kubernetes_pod_name]
            target_label: pod
          - source_labels: [__meta_kubernetes_namespace]
            target_label: namespace
          - source_labels: [__meta_kubernetes_pod_container_name]
            target_label: container
          - source_labels: [__meta_kubernetes_pod_node_name]
            target_label: node
          - source_labels: [__meta_kubernetes_pod_label_app_kubernetes_io_name]
            target_label: app
          - source_labels: [__meta_kubernetes_pod_label_app_kubernetes_io_component]
            target_label: component
        
        # 로그 파싱 파이프라인
        pipeline_stages:
          # JSON 로그 파싱 (Spring Boot)
          - json:
              expressions:
                timestamp: timestamp
                level: level
                logger: logger
                message: message
                thread: thread
                trace_id: traceId
                span_id: spanId
          
          # 타임스탬프 파싱
          - timestamp:
              source: timestamp
              format: RFC3339Nano
              fallback_formats:
                - "2006-01-02T15:04:05.000Z"
                - "2006-01-02 15:04:05"
          
          # 라벨 추가
          - labels:
              level:
              logger:
              trace_id:
              span_id:
          
          # 로그 레벨별 처리
          - match:
              selector: '{level="ERROR"}'
              stages:
              - labels:
                  severity: error
          
          - match:
              selector: '{level="WARN"}'
              stages:
              - labels:
                  severity: warning

# AlertManager 설정
alertmanager:
  enabled: true
  
  # 스토리지 설정
  persistence:
    enabled: true
    storageClass: gp3-encrypted
    size: 10Gi
  
  # 알림 설정
  config:
    global:
      slack_api_url: '${SLACK_WEBHOOK_URL}'
    
    route:
      group_by: ['alertname', 'cluster', 'service']
      group_wait: 10s
      group_interval: 10s
      repeat_interval: 12h
      receiver: 'default'
      routes:
      - match:
          severity: critical
        receiver: 'critical-alerts'
        group_wait: 5s
        repeat_interval: 5m
    
    receivers:
    - name: 'default'
      slack_configs:
      - channel: '#alerts'
        title: 'Popcorn MSA Alert'
        text: '{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}'
        send_resolved: true
    
    - name: 'critical-alerts'
      slack_configs:
      - channel: '#critical-alerts'
        title: '🚨 CRITICAL: {{ .GroupLabels.alertname }}'
        text: |
          {{ range .Alerts }}
          *Alert:* {{ .Annotations.summary }}
          *Description:* {{ .Annotations.description }}
          *Service:* {{ .Labels.service }}
          {{ end }}
        send_resolved: true
```

#### 1.3 커스텀 메트릭 수집 설정

**ServiceMonitor 설정**:
```yaml
# k8s/monitoring/servicemonitors.yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: popcorn-services
  namespace: monitoring
  labels:
    app: popcorn-services
    release: prometheus
spec:
  selector:
    matchLabels:
      monitoring: enabled
  namespaceSelector:
    matchNames:
    - popcorn
  endpoints:
  - port: management
    path: /actuator/prometheus
    interval: 30s
    scrapeTimeout: 10s
    honorLabels: true
---
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: kafka-jmx
  namespace: monitoring
  labels:
    app: kafka-jmx
    release: prometheus
spec:
  selector:
    matchLabels:
      app: kafka-jmx-exporter
  endpoints:
  - port: metrics
    interval: 30s
    path: /metrics
```

### Day 4-5: 외부 시스템 모니터링 설정

#### 4.1 PostgreSQL Exporter 설치

**PostgreSQL Exporter 배포**:
```yaml
# k8s/monitoring/postgres-exporter.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres-exporter
  namespace: monitoring
  labels:
    app: postgres-exporter
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres-exporter
  template:
    metadata:
      labels:
        app: postgres-exporter
    spec:
      containers:
      - name: postgres-exporter
        image: prometheuscommunity/postgres-exporter:v0.12.0
        ports:
        - containerPort: 9187
          name: metrics
        env:
        - name: DATA_SOURCE_NAME
          valueFrom:
            secretKeyRef:
              name: postgres-exporter-secret
              key: data-source-name
        resources:
          requests:
            cpu: 100m
            memory: 128Mi
          limits:
            cpu: 200m
            memory: 256Mi
        livenessProbe:
          httpGet:
            path: /metrics
            port: 9187
          initialDelaySeconds: 30
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /metrics
            port: 9187
          initialDelaySeconds: 5
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: postgres-exporter
  namespace: monitoring
  labels:
    app: postgres-exporter
spec:
  ports:
  - port: 9187
    targetPort: 9187
    name: metrics
  selector:
    app: postgres-exporter
---
apiVersion: v1
kind: Secret
metadata:
  name: postgres-exporter-secret
  namespace: monitoring
type: Opaque
data:
  data-source-name: cG9zdGdyZXNxbDovL3VzZXI6cGFzc3dvcmRAaG9zdDo1NDMyL2RhdGFiYXNlP3NzbG1vZGU9ZGlzYWJsZQ==  # base64 encoded connection string
```

#### 4.2 Redis Exporter 설치

**Redis Exporter 배포**:
```yaml
# k8s/monitoring/redis-exporter.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis-exporter
  namespace: monitoring
  labels:
    app: redis-exporter
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis-exporter
  template:
    metadata:
      labels:
        app: redis-exporter
    spec:
      containers:
      - name: redis-exporter
        image: oliver006/redis_exporter:v1.50.0
        ports:
        - containerPort: 9121
          name: metrics
        env:
        - name: REDIS_ADDR
          value: "${REDIS_ENDPOINT}:6379"
        resources:
          requests:
            cpu: 100m
            memory: 128Mi
          limits:
            cpu: 200m
            memory: 256Mi
---
apiVersion: v1
kind: Service
metadata:
  name: redis-exporter
  namespace: monitoring
  labels:
    app: redis-exporter
spec:
  ports:
  - port: 9121
    targetPort: 9121
    name: metrics
  selector:
    app: redis-exporter
```

#### 4.3 Kafka JMX Exporter 설치

**Kafka JMX Exporter 배포**:
```yaml
# k8s/monitoring/kafka-jmx-exporter.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka-jmx-exporter
  namespace: monitoring
  labels:
    app: kafka-jmx-exporter
spec:
  replicas: 1
  selector:
    matchLabels:
      app: kafka-jmx-exporter
  template:
    metadata:
      labels:
        app: kafka-jmx-exporter
    spec:
      containers:
      - name: jmx-exporter
        image: sscaling/jmx-prometheus-exporter:0.17.0
        ports:
        - containerPort: 5556
          name: metrics
        env:
        - name: CONFIG_YML
          value: |
            startDelaySeconds: 0
            ssl: false
            lowercaseOutputName: false
            lowercaseOutputLabelNames: false
            rules:
            # Kafka Server metrics
            - pattern: "kafka.server<type=(.+), name=(.+)><>Value"
              name: kafka_server_$1_$2
              type: GAUGE
            
            # Kafka Network metrics
            - pattern: "kafka.network<type=(.+), name=(.+)><>Value"
              name: kafka_network_$1_$2
              type: GAUGE
            
            # Kafka Log metrics
            - pattern: "kafka.log<type=(.+), name=(.+)><>Value"
              name: kafka_log_$1_$2
              type: GAUGE
            
            # Consumer lag metrics
            - pattern: "kafka.consumer<type=(.+), name=(.+), client-id=(.+)><>Value"
              name: kafka_consumer_$1_$2
              labels:
                client_id: "$3"
              type: GAUGE
        command:
        - java
        - -XX:+UnlockExperimentalVMOptions
        - -XX:+UseCGroupMemoryLimitForHeap
        - -XX:MaxRAMFraction=1
        - -jar
        - jmx_prometheus_httpserver.jar
        - "5556"
        - /etc/jmx-exporter/config.yml
        resources:
          requests:
            cpu: 100m
            memory: 256Mi
          limits:
            cpu: 200m
            memory: 512Mi
---
apiVersion: v1
kind: Service
metadata:
  name: kafka-jmx-exporter
  namespace: monitoring
  labels:
    app: kafka-jmx-exporter
spec:
  ports:
  - port: 5556
    targetPort: 5556
    name: metrics
  selector:
    app: kafka-jmx-exporter
```

### Day 6-7: 알림 규칙 설정

#### 6.1 PrometheusRule 설정

**알림 규칙 정의**:
```yaml
# k8s/monitoring/prometheus-rules.yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: popcorn-alerts
  namespace: monitoring
  labels:
    prometheus: kube-prometheus
    role: alert-rules
    release: prometheus
spec:
  groups:
  - name: popcorn.sli.rules
    interval: 30s
    rules:
    # SLI 메트릭 계산
    - record: sli:http_request_rate
      expr: |
        sum(rate(http_requests_total{job=~".*-service"}[5m])) by (job, method, status)
    
    - record: sli:http_success_rate
      expr: |
        sum(rate(http_requests_total{job=~".*-service",status=~"2.."}[5m])) by (job) /
        sum(rate(http_requests_total{job=~".*-service"}[5m])) by (job)
    
    - record: sli:http_error_rate
      expr: |
        sum(rate(http_requests_total{job=~".*-service",status=~"5.."}[5m])) by (job) /
        sum(rate(http_requests_total{job=~".*-service"}[5m])) by (job)
    
    - record: sli:http_latency_p95
      expr: |
        histogram_quantile(0.95, 
          sum(rate(http_request_duration_seconds_bucket{job=~".*-service"}[5m])) by (le, job)
        )
    
    - record: sli:http_latency_p99
      expr: |
        histogram_quantile(0.99, 
          sum(rate(http_request_duration_seconds_bucket{job=~".*-service"}[5m])) by (le, job)
        )

  - name: popcorn.slo.alerts
    rules:
    # Gateway Service SLO 알림
    - alert: GatewayServiceAvailabilitySLOViolation
      expr: |
        sli:http_success_rate{job="gateway-service"} < 0.999
      for: 2m
      labels:
        severity: critical
        service: gateway-service
        slo_type: availability
        team: platform
      annotations:
        summary: "Gateway Service 가용성 SLO 위반"
        description: "Gateway Service의 가용성이 {{ $value | humanizePercentage }}로 SLO 99.9% 미만입니다."
        runbook_url: "https://wiki.popcorn.com/sre/runbooks/gateway-availability"
        dashboard_url: "https://grafana.popcorn.com/d/gateway-service"

    - alert: GatewayServiceLatencySLOViolation
      expr: |
        sli:http_latency_p95{job="gateway-service"} > 0.5
      for: 5m
      labels:
        severity: warning
        service: gateway-service
        slo_type: latency
        team: platform
      annotations:
        summary: "Gateway Service 응답시간 SLO 위반"
        description: "Gateway Service의 P95 응답시간이 {{ $value }}초로 SLO 500ms를 초과했습니다."
        runbook_url: "https://wiki.popcorn.com/sre/runbooks/gateway-latency"

    # Order Service SLO 알림 (비즈니스 크리티컬)
    - alert: OrderServiceAvailabilitySLOViolation
      expr: |
        sli:http_success_rate{job="order-service"} < 0.9995
      for: 1m
      labels:
        severity: critical
        service: order-service
        slo_type: availability
        team: backend
        page: "true"
      annotations:
        summary: "Order Service 가용성 SLO 위반"
        description: "Order Service의 가용성이 {{ $value | humanizePercentage }}로 SLO 99.95% 미만입니다."
        runbook_url: "https://wiki.popcorn.com/sre/runbooks/order-availability"

    - alert: OrderServiceErrorBudgetBurnRateHigh
      expr: |
        (
          sli:http_error_rate{job="order-service"} > 0.0005 * 2
        ) and (
          sli:http_error_rate{job="order-service"} > 0.0005 * 2
        )
      for: 2m
      labels:
        severity: critical
        service: order-service
        slo_type: error_budget
        team: backend
      annotations:
        summary: "Order Service Error Budget 빠른 소모"
        description: "Order Service의 Error Budget이 정상보다 2배 빠르게 소모되고 있습니다."
        runbook_url: "https://wiki.popcorn.com/sre/runbooks/error-budget"

    # Payment Service SLO 알림 (최고 수준)
    - alert: PaymentServiceCriticalSLOViolation
      expr: |
        sli:http_success_rate{job="payment-service"} < 0.9999
      for: 30s
      labels:
        severity: critical
        service: payment-service
        slo_type: availability
        team: backend
        page: "true"
      annotations:
        summary: "Payment Service 크리티컬 SLO 위반"
        description: "Payment Service의 가용성이 {{ $value | humanizePercentage }}로 크리티컬 SLO 99.99% 미만입니다. 즉시 대응 필요!"
        runbook_url: "https://wiki.popcorn.com/sre/runbooks/payment-critical"

  - name: popcorn.infrastructure.alerts
    rules:
    # 높은 CPU 사용률
    - alert: HighCPUUsage
      expr: |
        (
          rate(container_cpu_usage_seconds_total{namespace="popcorn", container!="POD", container!=""}[5m]) /
          container_spec_cpu_quota{namespace="popcorn", container!="POD", container!=""} * 
          container_spec_cpu_period{namespace="popcorn", container!="POD", container!=""} * 100
        ) > 80
      for: 5m
      labels:
        severity: warning
        team: platform
      annotations:
        summary: "높은 CPU 사용률 감지"
        description: "Pod {{ $labels.pod }}의 CPU 사용률이 {{ $value }}%입니다."

    # 높은 메모리 사용률
    - alert: HighMemoryUsage
      expr: |
        (
          container_memory_working_set_bytes{namespace="popcorn", container!="POD", container!=""} /
          container_spec_memory_limit_bytes{namespace="popcorn", container!="POD", container!=""} * 100
        ) > 80
      for: 5m
      labels:
        severity: warning
        team: platform
      annotations:
        summary: "높은 메모리 사용률 감지"
        description: "Pod {{ $labels.pod }}의 메모리 사용률이 {{ $value }}%입니다."

    # Pod 재시작 빈발
    - alert: PodRestartingTooMuch
      expr: |
        increase(kube_pod_container_status_restarts_total{namespace="popcorn"}[1h]) > 3
      for: 0m
      labels:
        severity: warning
        team: platform
      annotations:
        summary: "Pod 재시작 빈발"
        description: "Pod {{ $labels.pod }}이 지난 1시간 동안 {{ $value }}번 재시작되었습니다."

    # 노드 리소스 부족
    - alert: NodeResourceExhaustion
      expr: |
        (
          (kube_node_status_allocatable{resource="cpu"} - kube_node_status_allocatable{resource="cpu"}) /
          kube_node_status_allocatable{resource="cpu"} * 100
        ) > 90
      for: 5m
      labels:
        severity: critical
        team: platform
      annotations:
        summary: "노드 리소스 고갈"
        description: "노드 {{ $labels.node }}의 CPU 할당률이 90%를 초과했습니다."

  - name: popcorn.database.alerts
    rules:
    # 데이터베이스 연결 수 높음
    - alert: DatabaseConnectionHigh
      expr: |
        (
          pg_stat_activity_count{state="active"} /
          pg_settings_max_connections * 100
        ) > 80
      for: 5m
      labels:
        severity: warning
        team: backend
      annotations:
        summary: "데이터베이스 연결 수 높음"
        description: "PostgreSQL 연결 사용률이 {{ $value }}%입니다."

    # 데이터베이스 응답 시간 높음
    - alert: DatabaseSlowQueries
      expr: |
        pg_stat_activity_max_tx_duration{datname="popcorn"} > 30
      for: 2m
      labels:
        severity: warning
        team: backend
      annotations:
        summary: "데이터베이스 느린 쿼리 감지"
        description: "PostgreSQL에서 30초 이상 실행되는 쿼리가 감지되었습니다."

  - name: popcorn.kafka.alerts
    rules:
    # Kafka Consumer Lag
    - alert: KafkaConsumerLag
      expr: kafka_consumer_lag_sum > 1000
      for: 5m
      labels:
        severity: warning
        team: backend
      annotations:
        summary: "Kafka Consumer Lag 높음"
        description: "토픽 {{ $labels.topic }}의 Consumer Lag이 {{ $value }}입니다."

    # Kafka Broker Down
    - alert: KafkaBrokerDown
      expr: up{job="kafka-jmx"} == 0
      for: 1m
      labels:
        severity: critical
        team: platform
      annotations:
        summary: "Kafka 브로커 다운"
        description: "Kafka 브로커 {{ $labels.instance }}가 다운되었습니다."
```

---

## 📝 Week 2: 로깅 및 분산 추적 시스템

### Day 8-10: Loki 로깅 시스템 구축

#### 8.1 Loki Stack 설치

**Loki 설치 스크립트**:
```bash
#!/bin/bash
# scripts/install-loki-stack.sh

NAMESPACE="monitoring"
RELEASE_NAME="loki"

echo "📝 Loki 로깅 스택 설치"

# Grafana Helm 레포지토리 추가
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

# Loki Stack 설치
helm install $RELEASE_NAME grafana/loki-stack \
  --namespace $NAMESPACE \
  --values loki-values.yaml \
  --wait --timeout=10m

# 설치 확인
kubectl get pods -n $NAMESPACE -l app=loki
kubectl get pods -n $NAMESPACE -l app=promtail

echo "✅ Loki 스택 설치 완료"
```

**loki-values.yaml**:
```yaml
# Loki 설정
loki:
  enabled: true
  
  # 영구 스토리지
  persistence:
    enabled: true
    storageClassName: gp3-encrypted
    size: 50Gi
  
  # 리소스 설정
  resources:
    requests:
      cpu: 100m
      memory: 256Mi
    limits:
      cpu: 500m
      memory: 512Mi
  
  # Loki 설정
  config:
    auth_enabled: false
    
    server:
      http_listen_port: 3100
      grpc_listen_port: 9096
    
    ingester:
      lifecycler:
        address: 127.0.0.1
        ring:
          kvstore:
            store: inmemory
          replication_factor: 1
        final_sleep: 0s
      chunk_idle_period: 5m
      chunk_retain_period: 30s
      max_transfer_retries: 0
    
    schema_config:
      configs:
        - from: 2020-10-24
          store: boltdb-shipper
          object_store: filesystem
          schema: v11
          index:
            prefix: index_
            period: 24h
    
    storage_config:
      boltdb_shipper:
        active_index_directory: /data/loki/boltdb-shipper-active
        cache_location: /data/loki/boltdb-shipper-cache
        shared_store: filesystem
      filesystem:
        directory: /data/loki/chunks
    
    limits_config:
      enforce_metric_name: false
      reject_old_samples: true
      reject_old_samples_max_age: 168h
      ingestion_rate_mb: 16
      ingestion_burst_size_mb: 32
      per_stream_rate_limit: 3MB
      per_stream_rate_limit_burst: 15MB
    
    chunk_store_config:
      max_look_back_period: 0s
    
    table_manager:
      retention_deletes_enabled: true
      retention_period: 168h  # 7일 보존

# Promtail 설정 (로그 수집기)
promtail:
  enabled: true
  
  # 리소스 설정
  resources:
    requests:
      cpu: 100m
      memory: 128Mi
    limits:
      cpu: 200m
      memory: 256Mi
  
  # Promtail 설정
  config:
    server:
      http_listen_port: 3101
    
    positions:
      filename: /tmp/positions.yaml
    
    clients:
      - url: http://loki:3100/loki/api/v1/push
    
    scrape_configs:
      # Kubernetes Pod 로그 수집
      - job_name: kubernetes-pods
        kubernetes_sd_configs:
          - role: pod
        
        relabel_configs:
          # Pod 메타데이터 추출
          - source_labels:
              - __meta_kubernetes_pod_controller_name
            regex: ([0-9a-z-.]+?)(-[0-9a-f]{8,10})?
            action: replace
            target_label: __tmp_controller_name
          
          - source_labels:
              - __meta_kubernetes_pod_label_app_kubernetes_io_name
              - __meta_kubernetes_pod_label_app
              - __tmp_controller_name
              - __meta_kubernetes_pod_name
            regex: ^;*([^;]+)(;.*)?$
            action: replace
            target_label: app
          
          - source_labels:
              - __meta_kubernetes_pod_label_app_kubernetes_io_instance
              - __meta_kubernetes_pod_label_release
            regex: ^;*([^;]+)(;.*)?$
            action: replace
            target_label: instance
          
          - source_labels:
              - __meta_kubernetes_pod_label_app_kubernetes_io_component
              - __meta_kubernetes_pod_label_component
            regex: ^;*([^;]+)(;.*)?$
            action: replace
            target_label: component
          
          - source_labels:
              - __meta_kubernetes_pod_node_name
            target_label: node_name
          
          - source_labels:
              - __meta_kubernetes_namespace
            target_label: namespace
          
          - source_labels:
              - __meta_kubernetes_pod_name
            target_label: pod
          
          - source_labels:
              - __meta_kubernetes_pod_container_name
            target_label: container
          
          - replacement: /var/log/pods/*$1/*.log
            separator: /
            source_labels:
              - __meta_kubernetes_pod_uid
              - __meta_kubernetes_pod_container_name
            target_label: __path__
        
        # 로그 파싱 파이프라인
        pipeline_stages:
          # JSON 로그 파싱
          - json:
              expressions:
                level: level
                timestamp: timestamp
                message: message
                logger: logger
                thread: thread
          
          # 타임스탬프 파싱
          - timestamp:
              source: timestamp
              format: RFC3339Nano
              fallback_formats:
                - "2006-01-02T15:04:05.000Z"
                - "2006-01-02 15:04:05"
          
          # 라벨 추가
          - labels:
              level:
              logger:
              thread:
          
          # 로그 레벨별 필터링 (선택적)
          - match:
              selector: '{level="ERROR"}'
              stages:
              - labels:
                  severity: error
          
          - match:
              selector: '{level="WARN"}'
              stages:
              - labels:
                  severity: warning

# Fluent Bit 비활성화 (Promtail 사용)
fluent-bit:
  enabled: false

# Filebeat 비활성화
filebeat:
  enabled: false

# Logstash 비활성화
logstash:
  enabled: false

# Grafana 통합 (이미 설치된 Grafana 사용)
grafana:
  enabled: false
```

### Day 11-12: 분산 추적 시스템 (Tempo) 구축

> **📋 참고**: 완전한 LGTM 스택 구축 가이드는 [LGTM_STACK_GUIDE.md](./LGTM_STACK_GUIDE.md)를 참조하세요.

#### 11.1 Tempo 통합 설정

**Tempo는 LGTM 스택의 핵심 구성 요소**로, OpenTelemetry와 완전 호환되며 Grafana에서 로그-메트릭-트레이스 상관관계 분석을 제공합니다.

**Tempo 주요 특징**:
- OpenTelemetry 네이티브 지원
- Grafana 완전 통합 (트레이스 → 로그, 트레이스 → 메트릭)
- S3 호환 스토리지 지원
- 자동 서비스 맵 생성
- 메트릭 생성기 (트레이스에서 RED 메트릭 추출)

**Tempo 설치 (LGTM 스택 일부)**: 설치

**Tempo 설치 (LGTM 스택 일부)**:
```bash
#!/bin/bash
# scripts/install-tempo-with-lgtm.sh

echo "🔍 Tempo 분산 추적 시스템 (LGTM 스택 일부) 설치"

# LGTM 스택과 함께 Tempo 설치
helm install lgtm grafana/lgtm-distributed \
  --namespace monitoring \
  --values lgtm-values.yaml \
  --wait --timeout=30m

# Tempo 상태 확인
kubectl get pods -n monitoring -l app.kubernetes.io/name=tempo

echo "✅ Tempo (LGTM 스택) 설치 완료"
```

**OpenTelemetry → Tempo 연동 설정**:
```yaml
# Spring Boot application.yml
management:
  tracing:
    sampling:
      probability: 0.1  # 10% 샘플링
  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces
      
# OpenTelemetry 자동 계측
otel:
  service:
    name: ${spring.application.name}
    version: ${app.version:1.0.0}
  exporter:
    otlp:
      endpoint: http://otel-collector:4317
  resource:
    attributes:
      deployment.environment: ${ENVIRONMENT:dev}
      service.namespace: popcorn
```

**jaeger-instance.yaml**:
```yaml
apiVersion: jaegertracing.io/v1
kind: Jaeger
metadata:
  name: jaeger
  namespace: observability
spec:
  strategy: production
  
  # Collector 설정
  collector:
    maxReplicas: 5
    resources:
      limits:
        cpu: 500m
        memory: 512Mi
      requests:
        cpu: 100m
        memory: 256Mi
    
    # 환경 변수
    options:
      collector:
        zipkin:
          host-port: ":9411"
  
  # Query 설정 (UI)
  query:
    replicas: 2
    resources:
      limits:
        cpu: 500m
        memory: 512Mi
      requests:
        cpu: 100m
        memory: 256Mi
    
    # UI 설정
    options:
      query:
        base-path: /jaeger
  
  # Agent 설정 (사이드카)
  agent:
    strategy: DaemonSet
    resources:
      limits:
        cpu: 200m
        memory: 256Mi
      requests:
        cpu: 100m
        memory: 128Mi
  
  # 스토리지 설정 (Elasticsearch)
  storage:
    type: elasticsearch
    elasticsearch:
      nodeCount: 3
      redundancyPolicy: SingleRedundancy
      
      # 리소스 설정
      resources:
        requests:
          cpu: 500m
          memory: 2Gi
        limits:
          cpu: 1000m
          memory: 2Gi
      
      # 스토리지 설정
      storage:
        storageClassName: gp3-encrypted
        size: 50Gi
      
      # 인덱스 설정
      esIndexCleaner:
        enabled: true
        numberOfDays: 7
        schedule: "55 23 * * *"
  
  # Ingress 설정
  ingress:
    enabled: true
    annotations:
      kubernetes.io/ingress.class: alb
      alb.ingress.kubernetes.io/scheme: internal
      alb.ingress.kubernetes.io/target-type: ip
      alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS": 443}]'
    hosts:
      - jaeger.popcorn.local
    tls:
      - secretName: jaeger-tls
        hosts:
          - jaeger.popcorn.local
```

### Day 13-14: LGTM 통합 대시보드 및 알림 최적화

#### 13.1 LGTM 통합 대시보드 생성

**Popcorn MSA LGTM 통합 대시보드**:
```json
{
  "dashboard": {
    "title": "Popcorn MSA - LGTM Integrated Dashboard",
    "tags": ["popcorn", "lgtm", "observability"],
    "timezone": "Asia/Seoul",
    "refresh": "30s",
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "panels": [
      {
        "title": "🎯 Service Health (Golden Signals)",
        "type": "stat",
        "gridPos": {"h": 6, "w": 24, "x": 0, "y": 0},
        "targets": [
          {
            "expr": "up{job=~\".*-service\"}",
            "legendFormat": "{{ job }}",
            "datasource": "Mimir"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "mappings": [
              {"options": {"0": {"text": "❌ Down", "color": "red"}}, "type": "value"},
              {"options": {"1": {"text": "✅ Up", "color": "green"}}, "type": "value"}
            ]
          }
        }
      },
      {
        "title": "📊 Request Rate (Mimir)",
        "type": "graph",
        "gridPos": {"h": 8, "w": 8, "x": 0, "y": 6},
        "targets": [
          {
            "expr": "sum(rate(http_requests_total{job=~\".*-service\"}[5m])) by (job)",
            "legendFormat": "{{ job }}",
            "datasource": "Mimir"
          }
        ]
      },
      {
        "title": "⚠️ Error Rate (Mimir)",
        "type": "graph",
        "gridPos": {"h": 8, "w": 8, "x": 8, "y": 6},
        "targets": [
          {
            "expr": "sum(rate(http_requests_total{job=~\".*-service\",status=~\"5..\"}[5m])) by (job) / sum(rate(http_requests_total{job=~\".*-service\"}[5m])) by (job) * 100",
            "legendFormat": "{{ job }}",
            "datasource": "Mimir"
          }
        ],
        "thresholds": [
          {"value": 1, "colorMode": "critical", "op": "gt"}
        ]
      },
      {
        "title": "⏱️ Response Time P95 (Mimir)",
        "type": "graph",
        "gridPos": {"h": 8, "w": 8, "x": 16, "y": 6},
        "targets": [
          {
            "expr": "histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket{job=~\".*-service\"}[5m])) by (le, job)) * 1000",
            "legendFormat": "{{ job }}",
            "datasource": "Mimir"
          }
        ],
        "thresholds": [
          {"value": 500, "colorMode": "critical", "op": "gt"}
        ]
      },
      {
        "title": "📝 Recent Error Logs (Loki)",
        "type": "logs",
        "gridPos": {"h": 10, "w": 12, "x": 0, "y": 14},
        "targets": [
          {
            "expr": "{namespace=\"popcorn\", level=\"ERROR\"} |= \"\" | json | line_format \"{{.timestamp}} [{{.level}}] {{.logger}}: {{.message}} (trace={{.trace_id}})\"",
            "refId": "A",
            "datasource": "Loki"
          }
        ],
        "options": {
          "showTime": true,
          "showLabels": true,
          "wrapLogMessage": true,
          "enableLogDetails": true,
          "dedupStrategy": "exact"
        }
      },
      {
        "title": "🔍 Trace Volume & Latency (Tempo)",
        "type": "graph",
        "gridPos": {"h": 10, "w": 12, "x": 12, "y": 14},
        "targets": [
          {
            "expr": "sum(rate(traces_spanmetrics_calls_total[5m])) by (service_name)",
            "legendFormat": "Traces: {{ service_name }}",
            "datasource": "Mimir"
          },
          {
            "expr": "histogram_quantile(0.95, sum(rate(traces_spanmetrics_latency_bucket[5m])) by (le, service_name)) * 1000",
            "legendFormat": "P95 Latency: {{ service_name }}",
            "datasource": "Mimir"
          }
        ]
      },
      {
        "title": "🗺️ Service Dependency Map (Tempo)",
        "type": "nodeGraph",
        "gridPos": {"h": 12, "w": 24, "x": 0, "y": 24},
        "targets": [
          {
            "expr": "traces_spanmetrics_calls_total",
            "datasource": "Tempo"
          }
        ],
        "options": {
          "nodes": {
            "mainStatUnit": "short"
          },
          "edges": {
            "mainStatUnit": "reqps"
          }
        }
      },
      {
        "title": "🏥 LGTM Stack Health",
        "type": "stat",
        "gridPos": {"h": 6, "w": 24, "x": 0, "y": 36},
        "targets": [
          {
            "expr": "up{job=~\"loki.*\"}",
            "legendFormat": "Loki: {{ job }}",
            "datasource": "Mimir"
          },
          {
            "expr": "up{job=\"grafana\"}",
            "legendFormat": "Grafana",
            "datasource": "Mimir"
          },
          {
            "expr": "up{job=~\"tempo.*\"}",
            "legendFormat": "Tempo: {{ job }}",
            "datasource": "Mimir"
          },
          {
            "expr": "up{job=~\"mimir.*\"}",
            "legendFormat": "Mimir: {{ job }}",
            "datasource": "Mimir"
          }
        ]
      }
    ]
  }
}
```

#### 13.2 LGTM 통합 알림 규칙

**LGTM 스택 기반 알림 규칙**:
```yaml
# k8s/monitoring/lgtm-prometheus-rules.yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: lgtm-stack-alerts
  namespace: monitoring
  labels:
    prometheus: kube-prometheus
    role: alert-rules
    release: prometheus
spec:
  groups:
  - name: lgtm.stack.alerts
    interval: 30s
    rules:
    # LGTM 스택 컴포넌트 상태 알림
    - alert: LokiComponentDown
      expr: up{job=~"loki.*"} == 0
      for: 2m
      labels:
        severity: critical
        component: lgtm
        team: platform
      annotations:
        summary: "Loki 컴포넌트 다운"
        description: "Loki 컴포넌트 {{ $labels.job }}이 다운되었습니다."
        runbook_url: "https://wiki.popcorn.com/sre/runbooks/loki-down"
    
    - alert: TempoComponentDown
      expr: up{job=~"tempo.*"} == 0
      for: 2m
      labels:
        severity: critical
        component: lgtm
        team: platform
      annotations:
        summary: "Tempo 컴포넌트 다운"
        description: "Tempo 컴포넌트 {{ $labels.job }}이 다운되었습니다."
        runbook_url: "https://wiki.popcorn.com/sre/runbooks/tempo-down"
    
    - alert: MimirComponentDown
      expr: up{job=~"mimir.*"} == 0
      for: 2m
      labels:
        severity: critical
        component: lgtm
        team: platform
      annotations:
        summary: "Mimir 컴포넌트 다운"
        description: "Mimir 컴포넌트 {{ $labels.job }}이 다운되었습니다."
        runbook_url: "https://wiki.popcorn.com/sre/runbooks/mimir-down"
    
    - alert: GrafanaDown
      expr: up{job="grafana"} == 0
      for: 1m
      labels:
        severity: critical
        component: lgtm
        team: platform
      annotations:
        summary: "Grafana 다운"
        description: "Grafana가 다운되었습니다. 대시보드 접근 불가."
        runbook_url: "https://wiki.popcorn.com/sre/runbooks/grafana-down"

  - name: lgtm.performance.alerts
    rules:
    # 로그 수집 지연
    - alert: LokiIngestionLag
      expr: |
        (
          time() - loki_ingester_oldest_unshipped_block_timestamp_seconds > 300
        )
      for: 5m
      labels:
        severity: warning
        component: lgtm
        team: platform
      annotations:
        summary: "Loki 로그 수집 지연"
        description: "Loki에서 로그 수집이 5분 이상 지연되고 있습니다."
    
    # 트레이스 수집 지연
    - alert: TempoIngestionLag
      expr: |
        (
          time() - tempo_ingester_oldest_block_start_timestamp_seconds > 600
        )
      for: 5m
      labels:
        severity: warning
        component: lgtm
        team: platform
      annotations:
        summary: "Tempo 트레이스 수집 지연"
        description: "Tempo에서 트레이스 수집이 10분 이상 지연되고 있습니다."
    
    # 메트릭 수집 지연
    - alert: MimirIngestionLag
      expr: |
        (
          time() - mimir_ingester_oldest_unshipped_block_timestamp_seconds > 300
        )
      for: 5m
      labels:
        severity: warning
        component: lgtm
        team: platform
      annotations:
        summary: "Mimir 메트릭 수집 지연"
        description: "Mimir에서 메트릭 수집이 5분 이상 지연되고 있습니다."

  - name: lgtm.storage.alerts
    rules:
    # 스토리지 사용량 높음
    - alert: LGTMStorageUsageHigh
      expr: |
        (
          (
            kubelet_volume_stats_used_bytes{persistentvolumeclaim=~".*loki.*|.*tempo.*|.*mimir.*"} /
            kubelet_volume_stats_capacity_bytes{persistentvolumeclaim=~".*loki.*|.*tempo.*|.*mimir.*"}
          ) * 100 > 80
        )
      for: 10m
      labels:
        severity: warning
        component: lgtm
        team: platform
      annotations:
        summary: "LGTM 스택 스토리지 사용량 높음"
        description: "{{ $labels.persistentvolumeclaim }}의 스토리지 사용률이 {{ $value }}%입니다."
    
    # S3 버킷 접근 오류
    - alert: LGTMStorageError
      expr: |
        increase(loki_boltdb_shipper_upload_errors_total[5m]) > 0 or
        increase(tempo_storage_errors_total[5m]) > 0 or
        increase(mimir_blocks_storage_errors_total[5m]) > 0
      for: 2m
      labels:
        severity: critical
        component: lgtm
        team: platform
      annotations:
        summary: "LGTM 스택 스토리지 오류"
        description: "LGTM 스택에서 S3 스토리지 접근 오류가 발생했습니다."

  - name: lgtm.correlation.alerts
    rules:
    # 로그-트레이스 상관관계 누락
    - alert: LogTraceCorrelationMissing
      expr: |
        (
          sum(rate(loki_distributor_lines_received_total[5m])) > 0
        ) and (
          sum(rate(traces_spanmetrics_calls_total[5m])) == 0
        )
      for: 10m
      labels:
        severity: warning
        component: lgtm
        team: platform
      annotations:
        summary: "로그-트레이스 상관관계 누락"
        description: "로그는 수집되고 있지만 트레이스가 수집되지 않아 상관관계 분석이 불가능합니다."
    
    # 메트릭-트레이스 상관관계 누락
    - alert: MetricTraceCorrelationMissing
      expr: |
        (
          sum(rate(prometheus_tsdb_samples_received_total[5m])) > 0
        ) and (
          sum(rate(traces_spanmetrics_calls_total[5m])) == 0
        )
      for: 10m
      labels:
        severity: warning
        component: lgtm
        team: platform
      annotations:
        summary: "메트릭-트레이스 상관관계 누락"
        description: "메트릭은 수집되고 있지만 트레이스가 수집되지 않아 상관관계 분석이 불가능합니다."
```

**LGTM 스택 검증 체크리스트**:
- [ ] **Loki**: 로그 수집 및 LogQL 쿼리 정상 동작
- [ ] **Grafana**: LGTM 통합 대시보드 접속 및 데이터 표시 확인
- [ ] **Tempo**: 분산 트레이싱 데이터 수집 및 서비스 맵 생성 확인
- [ ] **Mimir**: 장기 메트릭 저장 및 PromQL 쿼리 정상 동작
- [ ] **통합 기능**: 트레이스 → 로그, 트레이스 → 메트릭 연결 확인
- [ ] **AlertManager**: LGTM 스택 알림 규칙 테스트
- [ ] **Slack 알림**: LGTM 관련 알림 수신 확인
- [ ] **OpenTelemetry**: 애플리케이션에서 OTLP 데이터 전송 확인
- [ ] **스토리지**: S3 버킷 데이터 저장 및 압축 확인
- [ ] **성능**: 대시보드 로딩 시간 및 쿼리 응답성 확인
- [ ] **보존 정책**: 데이터 보존 기간 및 자동 삭제 확인
- [ ] **상관관계**: 로그-메트릭-트레이스 간 연결 및 탐색 확인

## 🎯 LGTM 스택 운영 가이드

### 일일 운영 체크리스트
1. **LGTM 스택 상태 모니터링**
   - Loki, Grafana, Tempo, Mimir 컴포넌트 헬스체크
   - 스토리지 사용량 및 S3 버킷 상태 확인
   - 데이터 수집률 및 지연 시간 모니터링

2. **통합 관찰성 확인**
   - 로그-메트릭-트레이스 상관관계 정상 동작 확인
   - 서비스 맵 및 의존성 그래프 업데이트 확인
   - 알림 규칙 및 대시보드 정확성 검증

3. **성능 최적화**
   - 쿼리 성능 분석 및 최적화
   - 스토리지 압축 및 보존 정책 조정
   - 리소스 사용량 모니터링 및 스케일링

> **📚 추가 자료**: 
> - [LGTM_STACK_GUIDE.md](./LGTM_STACK_GUIDE.md) - 완전한 LGTM 스택 구축 가이드
> - [Grafana LGTM 공식 문서](https://grafana.com/docs/lgtm/)
> - [OpenTelemetry 통합 가이드](https://opentelemetry.io/docs/)

이 가이드를 통해 Monitoring Engineer는 체계적으로 **Grafana LGTM 스택**을 구축하고, 통합된 관찰성 환경에서 실시간 모니터링과 효과적인 알림 시스템을 통해 시스템의 안정성과 성능을 보장할 수 있습니다.