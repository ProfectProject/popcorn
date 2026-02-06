# Popcorn MSA Istio Service Mesh 마이그레이션 가이드

## 📋 개요

현재 Spring Cloud Gateway + ECS 기반 아키텍처를 Istio Service Mesh + EKS 기반으로 전환하는 마이그레이션 가이드입니다.

## 🎯 마이그레이션 목표

- **네트워크 통신 중앙화**: Istio Service Mesh를 통한 트래픽 관리
- **보안 강화**: mTLS 자동 암호화, 중앙화된 인증/인가
- **운영 효율성**: 인프라 레벨 resilience, 통합 모니터링
- **코드 단순화**: 애플리케이션에서 네트워크 로직 제거

## 🔄 현재 vs 목표 아키텍처

### 현재 아키텍처
```
[ALB] → [Spring Cloud Gateway] → [ECS Services]
                ↓
        [CloudMap DNS Discovery]
                ↓
        [Resilience4j Circuit Breaker]
```

### 목표 아키텍처
```
[Istio Ingress Gateway] → [Istio Service Mesh] → [EKS Pods]
                                ↓
                    [Envoy Sidecar Proxies]
                                ↓
                    [mTLS + Traffic Management]
```

## 📊 변경 사항 요약

| 구성 요소 | 현재 | 변경 후 | 상태 |
|-----------|------|---------|------|
| **컨테이너 오케스트레이션** | ECS | EKS | 🔄 마이그레이션 필요 |
| **API Gateway** | Spring Cloud Gateway | Istio Ingress Gateway | 🔄 변경 필요 |
| **서비스 디스커버리** | AWS CloudMap | Kubernetes Service | 🔄 변경 필요 |
| **로드밸런싱** | Spring Cloud LoadBalancer | Istio VirtualService | 🔄 변경 필요 |
| **Circuit Breaker** | Resilience4j | Istio DestinationRule | 🔄 변경 필요 |
| **Retry** | Resilience4j | Istio VirtualService | 🔄 변경 필요 |
| **Rate Limiting** | Resilience4j | Istio AuthorizationPolicy | 🔄 변경 필요 |
| **인증/인가** | JWT + Passport | JWT + mTLS | 🔄 단순화 필요 |
| **모니터링** | Spring Actuator | Istio + Prometheus + Jaeger | 🔄 강화 필요 |

---

## 🚀 Phase 1: Kubernetes 리소스 생성

### 1.1 Namespace 및 기본 설정

```yaml
# k8s/namespaces/popcorn-namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: popcorn
  labels:
    istio-injection: enabled  # Istio 사이드카 자동 주입
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: popcorn-config
  namespace: popcorn
data:
  DB_HOST: "popcorn-postgres.default.svc.cluster.local"
  REDIS_HOST: "popcorn-redis.default.svc.cluster.local"
  KAFKA_BROKER: "popcorn-kafka.default.svc.cluster.local:9092"
```

### 1.2 서비스별 Deployment 및 Service

```yaml
# k8s/services/user-service.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  namespace: popcorn
spec:
  replicas: 2
  selector:
    matchLabels:
      app: user-service
      version: v1
  template:
    metadata:
      labels:
        app: user-service
        version: v1
    spec:
      containers:
      - name: user-service
        image: your-registry/user-service:latest
        ports:
        - containerPort: 8082
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "k8s"
        - name: DB_HOST
          valueFrom:
            configMapKeyRef:
              name: popcorn-config
              key: DB_HOST
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8082
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8082
          initialDelaySeconds: 5
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: user-service
  namespace: popcorn
  labels:
    app: user-service
spec:
  selector:
    app: user-service
  ports:
  - port: 8082
    targetPort: 8082
    name: http
```

### 1.3 Gateway Service (Spring Cloud Gateway 유지)

```yaml
# k8s/services/gateway-service.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway-service
  namespace: popcorn
spec:
  replicas: 2
  selector:
    matchLabels:
      app: gateway-service
      version: v1
  template:
    metadata:
      labels:
        app: gateway-service
        version: v1
    spec:
      containers:
      - name: gateway-service
        image: your-registry/gateway-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "k8s"
        # 서비스 URL을 Kubernetes Service로 변경
        - name: USER_SERVICE_URL
          value: "http://user-service:8082"
        - name: ORDER_SERVICE_URL
          value: "http://order-service:8084"
        - name: PAYMENT_SERVICE_URL
          value: "http://payment-service:8085"
        - name: STORE_SERVICE_URL
          value: "http://store-service:8083"
        - name: CHECKIN_SERVICE_URL
          value: "http://checkin-service:8086"
        - name: ORDERQUERY_SERVICE_URL
          value: "http://orderquery-service:8087"
---
apiVersion: v1
kind: Service
metadata:
  name: gateway-service
  namespace: popcorn
spec:
  selector:
    app: gateway-service
  ports:
  - port: 8080
    targetPort: 8080
    name: http
```

---

## 🌐 Phase 2: Istio 네트워킹 설정

### 2.1 Istio Gateway (외부 트래픽 진입점)

```yaml
# istio/gateway.yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: popcorn-gateway
  namespace: popcorn
spec:
  selector:
    istio: ingressgateway
  servers:
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - "api.goormpopcorn.shop"
    - "dev-api.goormpopcorn.shop"
  - port:
      number: 443
      name: https
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: popcorn-tls-secret
    hosts:
    - "api.goormpopcorn.shop"
    - "dev-api.goormpopcorn.shop"
```

### 2.2 VirtualService (트래픽 라우팅)

```yaml
# istio/virtualservice-gateway.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: popcorn-gateway-vs
  namespace: popcorn
spec:
  hosts:
  - "api.goormpopcorn.shop"
  - "dev-api.goormpopcorn.shop"
  gateways:
  - popcorn-gateway
  http:
  - match:
    - uri:
        prefix: "/api/"
    route:
    - destination:
        host: gateway-service
        port:
          number: 8080
    timeout: 30s
    retries:
      attempts: 3
      perTryTimeout: 10s
      retryOn: 5xx,reset,connect-failure,refused-stream
---
# istio/virtualservice-internal.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: order-service-vs
  namespace: popcorn
spec:
  hosts:
  - order-service
  http:
  - route:
    - destination:
        host: order-service
        port:
          number: 8084
    timeout: 30s
    retries:
      attempts: 2
      perTryTimeout: 10s
      retryOn: 5xx,reset,connect-failure
```

### 2.3 DestinationRule (트래픽 정책)

```yaml
# istio/destinationrule.yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: order-service-dr
  namespace: popcorn
spec:
  host: order-service
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        http1MaxPendingRequests: 100
        http2MaxRequests: 100
        maxRequestsPerConnection: 2
        maxRetries: 3
        consecutiveGatewayErrors: 5
        interval: 30s
        baseEjectionTime: 30s
    loadBalancer:
      simple: ROUND_ROBIN
    outlierDetection:
      consecutiveGatewayErrors: 5
      consecutive5xxErrors: 5
      interval: 30s
      baseEjectionTime: 30s
      maxEjectionPercent: 50
  subsets:
  - name: v1
    labels:
      version: v1
---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: payment-service-dr
  namespace: popcorn
spec:
  host: payment-service
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 50
      http:
        http1MaxPendingRequests: 50
        http2MaxRequests: 50
        maxRequestsPerConnection: 1
        maxRetries: 2
        consecutiveGatewayErrors: 3
        interval: 30s
        baseEjectionTime: 30s
    loadBalancer:
      simple: ROUND_ROBIN
    outlierDetection:
      consecutiveGatewayErrors: 3
      consecutive5xxErrors: 3
      interval: 30s
      baseEjectionTime: 30s
      maxEjectionPercent: 50
```

---

## 🔐 Phase 3: 보안 설정

### 3.1 mTLS 설정

```yaml
# istio/security/peer-authentication.yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: popcorn
spec:
  mtls:
    mode: STRICT  # 모든 서비스 간 mTLS 강제
---
# 외부 트래픽을 위한 예외 설정
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: gateway-permissive
  namespace: popcorn
spec:
  selector:
    matchLabels:
      app: gateway-service
  mtls:
    mode: PERMISSIVE  # Gateway는 외부 트래픽 허용
```

### 3.2 JWT 인증 설정

```yaml
# istio/security/request-authentication.yaml
apiVersion: security.istio.io/v1beta1
kind: RequestAuthentication
metadata:
  name: jwt-auth
  namespace: popcorn
spec:
  selector:
    matchLabels:
      app: gateway-service
  jwtRules:
  - issuer: "popcorn-user-service"
    jwksUri: "http://user-service:8082/.well-known/jwks.json"
    audiences:
    - "popcorn-api"
    forwardOriginalToken: true
```

### 3.3 Authorization Policy

```yaml
# istio/security/authorization-policy.yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: popcorn-authz
  namespace: popcorn
spec:
  rules:
  # 공개 엔드포인트 (인증 불필요)
  - to:
    - operation:
        paths: ["/api/users/v1/auth/login", "/api/users/v1/auth/refresh", "/api/users/v1/users/signup"]
  - to:
    - operation:
        paths: ["/api/stores/v1/popups"]
  - to:
    - operation:
        paths: ["/actuator/health", "/actuator/health/*"]
  - to:
    - operation:
        paths: ["/swagger-ui/*", "/v3/api-docs/*"]
  
  # 인증 필요 엔드포인트
  - from:
    - source:
        requestPrincipals: ["popcorn-user-service/*"]
    to:
    - operation:
        paths: ["/api/orders/*", "/api/payments/*", "/api/qr/*", "/api/checkins/*"]
---
# 서비스 간 통신 정책
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: order-service-authz
  namespace: popcorn
spec:
  selector:
    matchLabels:
      app: order-service
  rules:
  # Gateway에서의 접근 허용
  - from:
    - source:
        principals: ["cluster.local/ns/popcorn/sa/default"]
        namespaces: ["popcorn"]
  # Payment Service에서의 접근 허용 (상태 업데이트)
  - from:
    - source:
        principals: ["cluster.local/ns/popcorn/sa/default"]
    to:
    - operation:
        methods: ["POST", "PUT"]
        paths: ["/api/orders/v1/*/status"]
```

---

## 🔧 Phase 4: 애플리케이션 코드 변경

### 4.1 Resilience4j 제거

```java
// order/src/main/java/com/popcorn/order/config/OrderResilienceConfig.java
// 전체 파일 삭제 또는 주석 처리

// Before (제거할 코드)
@Configuration
@EnableConfigurationProperties(OrderResilienceProperties.class)
public class OrderResilienceConfig {
    
    @Bean
    public CircuitBreaker paymentCircuitBreaker() {
        return CircuitBreaker.ofDefaults("payment");
    }
    
    @Bean
    public Retry paymentRetry() {
        return Retry.ofDefaults("payment");
    }
    
    @Bean
    public RateLimiter orderRateLimiter() {
        return RateLimiter.ofDefaults("order");
    }
}
```

### 4.2 WebClient 설정 단순화

```java
// order/src/main/java/com/popcorn/order/config/WebClientConfig.java
@Configuration
public class WebClientConfig {
    
    @Bean
    @Qualifier("paymentWebClient")
    public WebClient paymentWebClient(@Value("${services.payment.url}") String paymentServiceUrl) {
        return WebClient.builder()
                .baseUrl(paymentServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                // Istio가 타임아웃과 재시도를 처리하므로 단순화
                .build();
    }
    
    @Bean
    @Qualifier("userWebClient")
    public WebClient userWebClient(@Value("${services.user.url}") String userServiceUrl) {
        return WebClient.builder()
                .baseUrl(userServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
```

### 4.3 서비스 클라이언트 단순화

```java
// order/src/main/java/com/popcorn/order/client/PaymentClient.java
@Component
@Slf4j
public class PaymentClient {
    
    private final WebClient paymentWebClient;
    
    public PaymentClient(@Qualifier("paymentWebClient") WebClient paymentWebClient) {
        this.paymentWebClient = paymentWebClient;
    }
    
    // Before (Resilience4j 애노테이션 제거)
    // @CircuitBreaker(name = "payment", fallbackMethod = "fallbackCreatePayment")
    // @Retry(name = "payment")
    // @RateLimiter(name = "order")
    public Mono<PaymentResponse> createPayment(PaymentRequest request) {
        return paymentWebClient
                .post()
                .uri("/api/payments/v1/payments")
                .bodyValue(request)
                // 내부 서비스 호출 헤더 제거 (mTLS로 자동 인증)
                // .header("X-Internal-Call", "true")
                // .header("X-Internal-Service", "order")
                .retrieve()
                .bodyToMono(PaymentResponse.class)
                .doOnError(error -> log.error("Payment creation failed", error));
    }
    
    // Fallback 메서드 제거 (Istio Circuit Breaker가 처리)
    // public Mono<PaymentResponse> fallbackCreatePayment(PaymentRequest request, Exception ex) {
    //     return Mono.just(PaymentResponse.builder()
    //             .status("PENDING")
    //             .message("Payment service temporarily unavailable")
    //             .build());
    // }
}
```

### 4.4 Gateway JWT 필터 단순화

```java
// gateway/src/main/java/com/popcorn/gateway/jwt/JwtFilter.java
@Component
@Slf4j
public class JwtFilter implements GlobalFilter, Ordered {
    
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        
        // 인증 제외 경로 확인
        if (isExcludedPath(path)) {
            return chain.filter(exchange);
        }
        
        String token = extractToken(request);
        if (token == null) {
            return handleUnauthorized(exchange);
        }
        
        try {
            Claims claims = jwtUtil.validateToken(token);
            
            // Passport 생성 및 캐싱 제거 (Istio mTLS로 대체)
            // String passport = createPassport(claims);
            // cachePassport(passport, claims);
            
            // 사용자 정보를 헤더에 추가 (다운스트림 서비스용)
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Role", claims.get("role", String.class))
                    // .header("X-Passport", passport) // 제거
                    .build();
            
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
            
        } catch (Exception e) {
            log.error("JWT validation failed", e);
            return handleUnauthorized(exchange);
        }
    }
    
    // Passport 관련 메서드들 제거
    // private String createPassport(Claims claims) { ... }
    // private void cachePassport(String passport, Claims claims) { ... }
}
```

### 4.5 application.yaml 설정 변경

```yaml
# gateway/src/main/resources/application-k8s.yaml
spring:
  profiles:
    active: k8s
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://user-service:8082  # Kubernetes Service DNS
          predicates:
            - Path=/api/users/**
          filters:
            - RewritePath=/api/users/(?<segment>.*), /${segment}
            
        - id: order-service
          uri: http://order-service:8084
          predicates:
            - Path=/api/orders/**
          filters:
            - RewritePath=/api/orders/(?<segment>.*), /${segment}
            
        - id: payment-service
          uri: http://payment-service:8085
          predicates:
            - Path=/api/payments/**,/api/pay/**
          filters:
            - RewritePath=/api/payments/(?<segment>.*), /${segment}
            - RewritePath=/api/pay/(?<segment>.*), /${segment}
            
        - id: store-service
          uri: http://store-service:8083
          predicates:
            - Path=/api/stores/**
          filters:
            - RewritePath=/api/stores/(?<segment>.*), /${segment}
            
        - id: checkin-service
          uri: http://checkin-service:8086
          predicates:
            - Path=/api/qr/**,/api/checkins/**
          filters:
            - RewritePath=/api/qr/(?<segment>.*), /${segment}
            - RewritePath=/api/checkins/(?<segment>.*), /${segment}
            
        - id: orderquery-service
          uri: http://orderquery-service:8087
          predicates:
            - Path=/api/orderquery/**
          filters:
            - RewritePath=/api/orderquery/(?<segment>.*), /${segment}

# 서비스별 application-k8s.yaml
# order/src/main/resources/application-k8s.yaml
services:
  payment:
    url: http://payment-service:8085  # Kubernetes Service DNS
  user:
    url: http://user-service:8082
  store:
    url: http://store-service:8083

# Resilience4j 설정 제거
# resilience4j:
#   circuitbreaker: ...
#   retry: ...
#   ratelimiter: ...
```

---

## 📊 Phase 5: 모니터링 및 관찰성

### 5.1 Prometheus 설정

```yaml
# monitoring/prometheus-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
  namespace: istio-system
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
    scrape_configs:
    # Istio 메트릭 수집
    - job_name: 'istio-mesh'
      kubernetes_sd_configs:
      - role: endpoints
        namespaces:
          names:
          - popcorn
      relabel_configs:
      - source_labels: [__meta_kubernetes_service_name, __meta_kubernetes_endpoint_port_name]
        action: keep
        regex: istio-proxy;http-monitoring
    
    # 애플리케이션 메트릭 수집
    - job_name: 'popcorn-services'
      kubernetes_sd_configs:
      - role: endpoints
        namespaces:
          names:
          - popcorn
      relabel_configs:
      - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
```

### 5.2 Jaeger 분산 추적

```yaml
# monitoring/jaeger-tracing.yaml
apiVersion: telemetry.istio.io/v1alpha1
kind: Telemetry
metadata:
  name: default
  namespace: popcorn
spec:
  tracing:
  - providers:
    - name: jaeger
  - randomSamplingPercentage: 100  # 개발 환경에서는 100%, 프로덕션에서는 1-10%
```

### 5.3 Kiali 대시보드

```yaml
# monitoring/kiali-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: kiali
  namespace: istio-system
data:
  config.yaml: |
    server:
      web_root: /kiali
    auth:
      strategy: anonymous
    deployment:
      accessible_namespaces:
      - popcorn
      - istio-system
    external_services:
      prometheus:
        url: "http://prometheus:9090"
      jaeger:
        url: "http://jaeger-query:16686"
      grafana:
        url: "http://grafana:3000"
```

---

## 🚀 Phase 6: 배포 및 테스트

### 6.1 Helm Chart 구조

```
helm/
├── Chart.yaml
├── values.yaml
├── values-dev.yaml
├── values-prod.yaml
└── templates/
    ├── deployments/
    │   ├── gateway-deployment.yaml
    │   ├── user-service-deployment.yaml
    │   ├── order-service-deployment.yaml
    │   ├── payment-service-deployment.yaml
    │   ├── store-service-deployment.yaml
    │   ├── checkin-service-deployment.yaml
    │   └── orderquery-service-deployment.yaml
    ├── services/
    │   └── *.yaml
    ├── istio/
    │   ├── gateway.yaml
    │   ├── virtualservices.yaml
    │   ├── destinationrules.yaml
    │   └── security/
    │       ├── peer-authentication.yaml
    │       ├── request-authentication.yaml
    │       └── authorization-policy.yaml
    └── monitoring/
        ├── servicemonitor.yaml
        └── telemetry.yaml
```

### 6.2 배포 스크립트

```bash
#!/bin/bash
# scripts/deploy.sh

set -e

ENVIRONMENT=${1:-dev}
NAMESPACE="popcorn"

echo "🚀 Deploying Popcorn MSA to $ENVIRONMENT environment"

# 1. Namespace 생성
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -
kubectl label namespace $NAMESPACE istio-injection=enabled --overwrite

# 2. Secrets 생성
kubectl create secret generic popcorn-secrets \
  --from-env-file=.env.$ENVIRONMENT \
  --namespace=$NAMESPACE \
  --dry-run=client -o yaml | kubectl apply -f -

# 3. Helm 배포
helm upgrade --install popcorn-msa ./helm \
  --namespace=$NAMESPACE \
  --values=./helm/values-$ENVIRONMENT.yaml \
  --wait \
  --timeout=10m

# 4. 배포 상태 확인
kubectl get pods -n $NAMESPACE
kubectl get services -n $NAMESPACE
kubectl get virtualservices -n $NAMESPACE
kubectl get destinationrules -n $NAMESPACE

echo "✅ Deployment completed successfully"
```

### 6.3 테스트 스크립트

```bash
#!/bin/bash
# scripts/test-istio.sh

GATEWAY_URL=$(kubectl get svc istio-ingressgateway -n istio-system -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
NAMESPACE="popcorn"

echo "🧪 Testing Istio Service Mesh"
echo "Gateway URL: $GATEWAY_URL"

# 1. 헬스체크 테스트
echo "1. Health Check Test"
curl -f http://$GATEWAY_URL/api/users/actuator/health || echo "❌ Health check failed"

# 2. 인증 테스트
echo "2. Authentication Test"
# 로그인
LOGIN_RESPONSE=$(curl -s -X POST http://$GATEWAY_URL/api/users/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.accessToken')

if [ "$TOKEN" != "null" ]; then
  echo "✅ Login successful"
  
  # 인증이 필요한 API 테스트
  curl -f -H "Authorization: Bearer $TOKEN" \
    http://$GATEWAY_URL/api/orders/v1/orders || echo "❌ Authenticated API failed"
else
  echo "❌ Login failed"
fi

# 3. 서비스 메시 상태 확인
echo "3. Service Mesh Status"
kubectl get pods -n $NAMESPACE -o wide
kubectl get virtualservices -n $NAMESPACE
kubectl get destinationrules -n $NAMESPACE

# 4. mTLS 확인
echo "4. mTLS Status"
istioctl authn tls-check $(kubectl get pods -n $NAMESPACE -l app=order-service -o jsonpath='{.items[0].metadata.name}').popcorn payment-service.popcorn.svc.cluster.local

# 5. 트래픽 메트릭 확인
echo "5. Traffic Metrics"
kubectl exec -n istio-system $(kubectl get pods -n istio-system -l app=prometheus -o jsonpath='{.items[0].metadata.name}') -- \
  curl -s 'http://localhost:9090/api/v1/query?query=istio_requests_total{destination_service_name="order-service"}' | jq '.data.result[0].value[1]'

echo "✅ All tests completed"
```

---

## 📈 성능 및 모니터링

### 성능 비교 (예상)

| 메트릭 | 현재 (ECS) | Istio (EKS) | 변화 |
|--------|------------|-------------|------|
| **응답 시간 (P95)** | 200ms | 220ms | +10% (사이드카 오버헤드) |
| **처리량 (TPS)** | 1000 | 950 | -5% (프록시 처리) |
| **CPU 사용률** | 60% | 70% | +10% (Envoy 프록시) |
| **메모리 사용률** | 512MB | 640MB | +25% (사이드카 메모리) |
| **네트워크 지연** | 5ms | 7ms | +2ms (프록시 홉) |

### 모니터링 대시보드

1. **Kiali**: 서비스 메시 토폴로지 및 트래픽 흐름
2. **Jaeger**: 분산 추적 및 성능 분석
3. **Grafana**: 메트릭 시각화 및 알림
4. **Prometheus**: 메트릭 수집 및 저장

---

## 🎯 마이그레이션 체크리스트

### Phase 1: 준비 (1-2주)
- [ ] EKS 클러스터 구성
- [ ] Istio 설치 및 설정
- [ ] Helm Chart 작성
- [ ] CI/CD 파이프라인 업데이트

### Phase 2: 개발 환경 (2-3주)
- [ ] Kubernetes 리소스 생성
- [ ] Istio 네트워킹 설정
- [ ] 애플리케이션 코드 변경
- [ ] 개발 환경 배포 및 테스트

### Phase 3: 스테이징 환경 (1-2주)
- [ ] 성능 테스트
- [ ] 보안 테스트
- [ ] 모니터링 검증
- [ ] 문서 업데이트

### Phase 4: 프로덕션 (1주)
- [ ] 블루-그린 배포 준비
- [ ] 트래픽 점진적 전환
- [ ] 모니터링 및 알림 설정
- [ ] 롤백 계획 준비

---

## 🚨 주의사항 및 위험 요소

### 성능 영향
- **Envoy 사이드카 오버헤드**: CPU +10%, 메모리 +25%
- **네트워크 지연 증가**: 프록시 홉으로 인한 2-5ms 추가 지연
- **처리량 감소**: 5-10% 처리량 감소 가능

### 운영 복잡도
- **Kubernetes 운영 지식 필요**: Pod, Service, Ingress 등
- **Istio 문제 해결**: VirtualService, DestinationRule 디버깅
- **모니터링 도구 추가**: Kiali, Jaeger 학습 필요

### 마이그레이션 위험
- **서비스 다운타임**: 블루-그린 배포로 최소화
- **설정 오류**: 단계적 검증으로 위험 감소
- **성능 저하**: 충분한 테스트와 모니터링

---

## 📚 참고 자료

- [Istio 공식 문서](https://istio.io/latest/docs/)
- [Kubernetes 공식 문서](https://kubernetes.io/docs/)
- [Spring Cloud Gateway to Istio Migration](https://istio.io/latest/docs/ops/integrations/spiffe/)
- [Envoy Proxy 문서](https://www.envoyproxy.io/docs/)
- [Helm 공식 문서](https://helm.sh/docs/)

---

이 가이드를 통해 Popcorn MSA를 안전하고 효율적으로 Istio Service Mesh로 마이그레이션할 수 있습니다. 각 단계별로 충분한 테스트와 검증을 거쳐 진행하시기 바랍니다.