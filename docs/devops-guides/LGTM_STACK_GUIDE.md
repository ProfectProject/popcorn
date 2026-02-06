# Grafana LGTM 스택 완전 구축 가이드

## 🎯 개요

**Grafana LGTM 스택**은 현대적인 관찰성(Observability) 플랫폼으로, 로그, 메트릭, 트레이스를 통합 관리하는 완전한 솔루션입니다.

## 🔧 LGTM 스택 구성 요소

### 핵심 구성 요소

| 구성 요소 | 역할 | 주요 기능 | 데이터 보존 |
|-----------|------|-----------|-------------|
| **Loki** | 로그 집계 및 검색 | 라벨 기반 로그 인덱싱, LogQL 쿼리 | 7일 (운영), 3일 (개발) |
| **Grafana** | 통합 시각화 플랫폼 | 대시보드, 알림, 데이터소스 통합 | 설정 영구 저장 |
| **Tempo** | 분산 트레이싱 | OpenTelemetry 호환, 트레이스 저장 | 7일 (압축 저장) |
| **Mimir** | 장기 메트릭 저장 | Prometheus 호환, 고가용성 | 30일 (상세), 1년 (집계) |

### 아키텍처 다이어그램

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

## 🚀 설치 및 구성

### 1. LGTM 스택 통합 설치

**설치 스크립트**:
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

### 2. LGTM 스택 통합 설정

**lgtm-values.yaml** (완전한 LGTM 스택 설정):
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
  
  # 스토리지 설정 (S3 호환)
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
  
  # 보존 정책 및 제한
  limits_config:
    retention_period: 168h  # 7일
    ingestion_rate_mb: 16
    ingestion_burst_size_mb: 32
    max_streams_per_user: 10000
    max_line_size: 256000
    
  # 압축 설정
  compactor:
    enabled: true
    retention_enabled: true
    retention_delete_delay: 2h
    retention_delete_worker_count: 150

# Grafana 설정 (중앙 허브)
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
  
  # LGTM 통합 플러그인
  plugins:
    - grafana-piechart-panel
    - grafana-worldmap-panel
    - grafana-clock-panel
    - grafana-polystat-panel
    - grafana-tempo-panel
    - grafana-loki-panel
  
  # 통합 데이터소스 자동 설정
  datasources:
    datasources.yaml:
      apiVersion: 1
      datasources:
      # Mimir (Prometheus 호환 메트릭)
      - name: Mimir
        type: prometheus
        url: http://mimir-query-frontend:8080/prometheus
        access: proxy
        isDefault: true
        jsonData:
          timeInterval: 30s
          queryTimeout: 60s
          httpMethod: POST
        editable: false
      
      # Loki (로그)
      - name: Loki
        type: loki
        url: http://loki-query-frontend:3100
        access: proxy
        jsonData:
          maxLines: 1000
          derivedFields:
            - datasourceUid: tempo
              matcherRegex: "traceID=(\\w+)"
              name: TraceID
              url: "${__value.raw}"
              urlDisplayLabel: "View Trace"
        editable: false
      
      # Tempo (분산 트레이싱)
      - name: Tempo
        type: tempo
        url: http://tempo-query-frontend:3100
        access: proxy
        uid: tempo
        jsonData:
          # 트레이스-로그 연결
          tracesToLogs:
            datasourceUid: loki
            tags: ['job', 'instance', 'pod', 'namespace']
            mappedTags: [{ key: 'service.name', value: 'service' }]
            mapTagNamesEnabled: false
            spanStartTimeShift: '1h'
            spanEndTimeShift: '1h'
            filterByTraceID: true
            filterBySpanID: false
          
          # 트레이스-메트릭 연결
          tracesToMetrics:
            datasourceUid: mimir
            tags: [{ key: 'service.name', value: 'service' }, { key: 'job' }]
            queries:
              - name: 'Request Rate'
                query: 'sum(rate(http_requests_total{$__tags}[5m]))'
              - name: 'Request Duration'
                query: 'histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket{$__tags}[5m])) by (le))'
          
          # 서비스 맵
          serviceMap:
            datasourceUid: mimir
          
          # 검색 설정
          search:
            hide: false
          
          # 노드 그래프
          nodeGraph:
            enabled: true
        editable: false

# Tempo 설정 (분산 트레이싱)
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
        insecure: false
  
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
  
  # 압축기 설정
  compactor:
    replicas: 1
    resources:
      requests:
        cpu: 100m
        memory: 512Mi
      limits:
        cpu: 500m
        memory: 1Gi
  
  # 보존 정책
  retention: 168h  # 7일
  
  # 메트릭 생성기 (트레이스에서 메트릭 추출)
  metricsGenerator:
    enabled: true
    replicas: 1
    resources:
      requests:
        cpu: 100m
        memory: 256Mi
      limits:
        cpu: 500m
        memory: 512Mi
    config:
      processor:
        span_metrics:
          dimensions:
            - service.name
            - service.namespace
            - service.version
            - deployment.environment
            - http.method
            - http.status_code
        service_graphs:
          dimensions:
            - service.name
            - service.namespace

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
  
  # 압축기
  compactor:
    replicas: 1
    persistence:
      enabled: true
      storageClass: gp3-encrypted
      size: 100Gi
    resources:
      requests:
        cpu: 500m
        memory: 1Gi
      limits:
        cpu: 1000m
        memory: 2Gi
  
  # 보존 정책
  limits:
    compactor_blocks_retention_period: 30d  # 30일 보존
    ingestion_rate: 20000
    ingestion_burst_size: 200000
    max_series_per_user: 1500000
    max_samples_per_query: 50000000
    max_query_parallelism: 32

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
        tenant_id: popcorn
    
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
          - source_labels: [__meta_kubernetes_pod_label_app_kubernetes_io_version]
            target_label: version
        
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
                service_name: service.name
          
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
              service_name:
          
          # 로그 레벨별 처리
          - match:
              selector: '{level="ERROR"}'
              stages:
              - labels:
                  severity: error
              - metrics:
                  error_total:
                    type: Counter
                    description: "Total number of error logs"
                    config:
                      action: inc
          
          - match:
              selector: '{level="WARN"}'
              stages:
              - labels:
                  severity: warning
```

### 3. OpenTelemetry Collector 설정

**otel-collector-values.yaml**:
```yaml
# OpenTelemetry Collector 설정
mode: deployment

# 리소스 설정
resources:
  requests:
    cpu: 200m
    memory: 256Mi
  limits:
    cpu: 500m
    memory: 512Mi

# 복제본 수
replicaCount: 2

# 설정
config:
  receivers:
    # OTLP 수신기
    otlp:
      protocols:
        grpc:
          endpoint: 0.0.0.0:4317
        http:
          endpoint: 0.0.0.0:4318
    
    # Jaeger 수신기 (호환성)
    jaeger:
      protocols:
        grpc:
          endpoint: 0.0.0.0:14250
        thrift_http:
          endpoint: 0.0.0.0:14268
        thrift_compact:
          endpoint: 0.0.0.0:6831
    
    # Zipkin 수신기 (호환성)
    zipkin:
      endpoint: 0.0.0.0:9411
    
    # Prometheus 메트릭 수신기
    prometheus:
      config:
        scrape_configs:
          - job_name: 'otel-collector'
            scrape_interval: 30s
            static_configs:
              - targets: ['0.0.0.0:8888']

  processors:
    # 배치 처리기
    batch:
      timeout: 1s
      send_batch_size: 1024
      send_batch_max_size: 2048
    
    # 메모리 제한기
    memory_limiter:
      limit_mib: 400
      spike_limit_mib: 100
      check_interval: 5s
    
    # 리소스 처리기
    resource:
      attributes:
        - key: deployment.environment
          value: ${ENVIRONMENT}
          action: upsert
        - key: service.namespace
          value: popcorn
          action: upsert
    
    # 스팬 메트릭 처리기
    spanmetrics:
      metrics_exporter: prometheus
      latency_histogram_buckets: [100us, 1ms, 2ms, 6ms, 10ms, 100ms, 250ms]
      dimensions:
        - name: http.method
          default: GET
        - name: http.status_code
        - name: service.name
        - name: service.version

  exporters:
    # Tempo로 트레이스 전송
    otlp/tempo:
      endpoint: http://tempo-distributor:4317
      tls:
        insecure: true
    
    # Mimir로 메트릭 전송
    prometheusremotewrite:
      endpoint: http://mimir-distributor:8080/api/v1/push
      tls:
        insecure: true
    
    # Loki로 로그 전송
    loki:
      endpoint: http://loki-distributor:3100/loki/api/v1/push
      tenant_id: popcorn
    
    # 디버깅용 로깅
    logging:
      loglevel: info

  service:
    pipelines:
      # 트레이스 파이프라인
      traces:
        receivers: [otlp, jaeger, zipkin]
        processors: [memory_limiter, resource, spanmetrics, batch]
        exporters: [otlp/tempo, logging]
      
      # 메트릭 파이프라인
      metrics:
        receivers: [otlp, prometheus]
        processors: [memory_limiter, resource, batch]
        exporters: [prometheusremotewrite, logging]
      
      # 로그 파이프라인
      logs:
        receivers: [otlp]
        processors: [memory_limiter, resource, batch]
        exporters: [loki, logging]

# 서비스 설정
service:
  type: ClusterIP
  ports:
    - name: otlp-grpc
      port: 4317
      targetPort: 4317
      protocol: TCP
    - name: otlp-http
      port: 4318
      targetPort: 4318
      protocol: TCP
    - name: jaeger-grpc
      port: 14250
      targetPort: 14250
      protocol: TCP
    - name: jaeger-thrift-http
      port: 14268
      targetPort: 14268
      protocol: TCP
    - name: jaeger-thrift-compact
      port: 6831
      targetPort: 6831
      protocol: UDP
    - name: zipkin
      port: 9411
      targetPort: 9411
      protocol: TCP
    - name: metrics
      port: 8888
      targetPort: 8888
      protocol: TCP

# 헬스체크
livenessProbe:
  httpGet:
    path: /
    port: 13133
readinessProbe:
  httpGet:
    path: /
    port: 13133
```

## 📊 LGTM 스택 통합 대시보드

### 1. 통합 관찰성 대시보드

**LGTM 통합 대시보드 JSON**:
```json
{
  "dashboard": {
    "title": "Popcorn MSA - LGTM Stack Overview",
    "tags": ["lgtm", "observability", "popcorn"],
    "timezone": "Asia/Seoul",
    "refresh": "30s",
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "panels": [
      {
        "title": "Service Health Overview",
        "type": "stat",
        "gridPos": {"h": 8, "w": 24, "x": 0, "y": 0},
        "targets": [
          {
            "expr": "up{job=~\".*-service\"}",
            "legendFormat": "{{ job }}",
            "refId": "A"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "mappings": [
              {"options": {"0": {"text": "❌ Down"}}, "type": "value"},
              {"options": {"1": {"text": "✅ Up"}}, "type": "value"}
            ],
            "thresholds": {
              "steps": [
                {"color": "red", "value": 0},
                {"color": "green", "value": 1}
              ]
            }
          }
        }
      },
      {
        "title": "Request Rate (Mimir)",
        "type": "graph",
        "gridPos": {"h": 8, "w": 8, "x": 0, "y": 8},
        "targets": [
          {
            "expr": "sum(rate(http_requests_total{job=~\".*-service\"}[5m])) by (job)",
            "legendFormat": "{{ job }}",
            "datasource": "Mimir"
          }
        ]
      },
      {
        "title": "Error Rate (Mimir)",
        "type": "graph",
        "gridPos": {"h": 8, "w": 8, "x": 8, "y": 8},
        "targets": [
          {
            "expr": "sum(rate(http_requests_total{job=~\".*-service\",status=~\"5..\"}[5m])) by (job) / sum(rate(http_requests_total{job=~\".*-service\"}[5m])) by (job) * 100",
            "legendFormat": "{{ job }}",
            "datasource": "Mimir"
          }
        ]
      },
      {
        "title": "Response Time P95 (Mimir)",
        "type": "graph",
        "gridPos": {"h": 8, "w": 8, "x": 16, "y": 8},
        "targets": [
          {
            "expr": "histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket{job=~\".*-service\"}[5m])) by (le, job)) * 1000",
            "legendFormat": "{{ job }}",
            "datasource": "Mimir"
          }
        ]
      },
      {
        "title": "Recent Error Logs (Loki)",
        "type": "logs",
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 16},
        "targets": [
          {
            "expr": "{namespace=\"popcorn\", level=\"ERROR\"} |= \"\"",
            "refId": "A",
            "datasource": "Loki"
          }
        ],
        "options": {
          "showTime": true,
          "showLabels": true,
          "showCommonLabels": false,
          "wrapLogMessage": true,
          "prettifyLogMessage": false,
          "enableLogDetails": true,
          "dedupStrategy": "none",
          "sortOrder": "Descending"
        }
      },
      {
        "title": "Trace Volume (Tempo)",
        "type": "graph",
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 16},
        "targets": [
          {
            "expr": "sum(rate(traces_spanmetrics_calls_total[5m])) by (service_name)",
            "legendFormat": "{{ service_name }}",
            "datasource": "Mimir"
          }
        ]
      },
      {
        "title": "Service Map",
        "type": "nodeGraph",
        "gridPos": {"h": 12, "w": 24, "x": 0, "y": 24},
        "targets": [
          {
            "expr": "traces_spanmetrics_calls_total",
            "datasource": "Tempo"
          }
        ]
      }
    ]
  }
}
```

### 2. LGTM 스택 상태 모니터링 대시보드

**LGTM 스택 헬스 대시보드**:
```json
{
  "dashboard": {
    "title": "LGTM Stack Health Dashboard",
    "tags": ["lgtm", "infrastructure", "monitoring"],
    "panels": [
      {
        "title": "Loki Components Status",
        "type": "stat",
        "targets": [
          {
            "expr": "up{job=~\"loki.*\"}",
            "legendFormat": "{{ job }}"
          }
        ]
      },
      {
        "title": "Grafana Status",
        "type": "stat",
        "targets": [
          {
            "expr": "up{job=\"grafana\"}",
            "legendFormat": "Grafana"
          }
        ]
      },
      {
        "title": "Tempo Components Status",
        "type": "stat",
        "targets": [
          {
            "expr": "up{job=~\"tempo.*\"}",
            "legendFormat": "{{ job }}"
          }
        ]
      },
      {
        "title": "Mimir Components Status",
        "type": "stat",
        "targets": [
          {
            "expr": "up{job=~\"mimir.*\"}",
            "legendFormat": "{{ job }}"
          }
        ]
      },
      {
        "title": "Storage Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "loki_ingester_memory_streams",
            "legendFormat": "Loki Streams"
          },
          {
            "expr": "tempo_ingester_live_traces",
            "legendFormat": "Tempo Live Traces"
          },
          {
            "expr": "mimir_ingester_memory_series",
            "legendFormat": "Mimir Series"
          }
        ]
      }
    ]
  }
}
```

## 🔍 LGTM 스택 운영 가이드

### 일일 운영 체크리스트

- [ ] **Loki**: 로그 수집률 및 인덱싱 상태 확인
- [ ] **Grafana**: 대시보드 로딩 시간 및 쿼리 성능 확인
- [ ] **Tempo**: 트레이스 수집률 및 압축 상태 확인
- [ ] **Mimir**: 메트릭 수집률 및 압축기 상태 확인
- [ ] **스토리지**: S3 버킷 사용량 및 비용 모니터링
- [ ] **알림**: AlertManager 규칙 및 Slack 알림 테스트

### 문제 해결 가이드

#### Loki 문제 해결
```bash
# Loki 컴포넌트 상태 확인
kubectl get pods -n monitoring -l app.kubernetes.io/name=loki

# Loki 로그 확인
kubectl logs -n monitoring -l app.kubernetes.io/component=distributor

# 로그 수집 상태 확인
kubectl logs -n monitoring -l app.kubernetes.io/name=promtail
```

#### Tempo 문제 해결
```bash
# Tempo 컴포넌트 상태 확인
kubectl get pods -n monitoring -l app.kubernetes.io/name=tempo

# 트레이스 수집 확인
kubectl port-forward -n monitoring svc/tempo-query-frontend 3100:3100
curl http://localhost:3100/api/search
```

#### Mimir 문제 해결
```bash
# Mimir 컴포넌트 상태 확인
kubectl get pods -n monitoring -l app.kubernetes.io/name=mimir

# 메트릭 수집 확인
kubectl port-forward -n monitoring svc/mimir-query-frontend 8080:8080
curl http://localhost:8080/prometheus/api/v1/query?query=up
```

## 📈 성능 최적화

### 스토리지 최적화
- **Loki**: 라벨 카디널리티 최소화, 압축 설정 조정
- **Tempo**: 샘플링 비율 조정, 압축 알고리즘 최적화
- **Mimir**: 블록 크기 조정, 압축기 스케줄링 최적화

### 쿼리 성능 최적화
- **LogQL**: 라벨 필터 우선 사용, 정규식 최소화
- **PromQL**: 메트릭 집계 최적화, 시간 범위 조정
- **TraceQL**: 서비스 및 스팬 필터 활용

이 LGTM 스택 가이드를 통해 완전한 관찰성 환경을 구축하고 운영할 수 있습니다.