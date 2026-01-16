# 🔥 Popcorn Backend - AOP 어노테이션 시스템 구현 가이드

## 📋 개요

Spring Boot 백엔드에 **엔터프라이즈급 AOP(Aspect-Oriented Programming) 어노테이션 시스템**을 구현하여 횡단 관심사(Cross-cutting Concerns)를 효과적으로 분리했습니다.

### 🎯 핵심 목표
- **보안 강화** 🔒: Rate Limiting, 입력 검증, 감사 로깅
- **성능 최적화** ⚡: 캐싱, 재시도 로직
- **운영 편의성** 📝: API 로깅, 메트릭 수집
- **개발 편의성** 🛠️: 멱등성 보장, 선언적 프로그래밍

## 🏗️ 아키텍처 구조

```
src/main/java/com/popcorn/demo/common/
├── annotation/          # 어노테이션 정의
│   ├── ApiLogging.java
│   ├── AuditLog.java
│   ├── CacheResult.java
│   ├── Idempotent.java
│   ├── RateLimit.java
│   ├── RetryOnFailure.java
│   └── ValidateRequest.java
├── aop/                # AOP Aspect 구현
│   ├── ApiLoggingAspect.java
│   ├── AuditLogAspect.java
│   ├── CacheResultAspect.java
│   ├── IdempotentAspect.java
│   ├── RateLimitAspect.java
│   ├── RetryAspect.java
│   └── ValidateRequestAspect.java
└── cache/              # 캐시 및 멱등성 서비스
    ├── IdempotencyService.java
    ├── IdempotentOperation.java
    └── CaffeineBasedIdempotencyService.java
```

## 🛡️ 핵심 어노테이션

### 1. @RateLimit - API 요청 제한
```java
@RateLimit(
    requests = 10,
    window = 60,
    keyExpression = "#authentication.principal.userId",
    algorithm = RateLimit.Algorithm.SLIDING_WINDOW,
    errorMessage = "요청이 너무 많습니다."
)
```

**지원 알고리즘:**
- `FIXED_WINDOW`: 고정 시간 윈도우
- `SLIDING_WINDOW`: 슬라이딩 윈도우 (정밀한 제어)
- `TOKEN_BUCKET`: 토큰 버킷 (버스트 허용)

### 2. @ApiLogging - API 요청/응답 로깅
```java
@ApiLogging(
    message = "주문 생성",
    includeRequest = true,
    includeResponse = true,
    maskSensitiveData = true,
    level = ApiLogging.LogLevel.INFO
)
```

**보안 기능:**
- 민감정보 자동 마스킹 (카드번호, 이메일 등)
- 실행 시간 측정
- 예외 정보 포함

### 3. @AuditLog - 감사 로깅
```java
@AuditLog(
    action = "ORDER_CREATE",
    resource = "ORDER",
    userIdExpression = "#authentication.principal.userId",
    resourceIdExpression = "#request.orderId",
    level = AuditLog.Level.INFO
)
```

**컴플라이언스 지원:**
- 구조화된 JSON 로그
- 사용자 추적
- 리소스 변경 기록

### 4. @Idempotent - 멱등성 보장
```java
@Idempotent(
    keyExpression = "#authentication.principal.userId + ':create_order:' + T(java.time.LocalDate).now()",
    keyPrefix = "order_creation",
    responseType = CreateOrderResponse.class
)
```

**중복 방지:**
- 동일 요청 중복 실행 차단
- 캐시 기반 결과 반환
- SpEL 표현식 지원

### 5. @CacheResult - 메서드 결과 캐싱
```java
@CacheResult(
    cacheName = "orderCache",
    keyExpression = "#orderId",
    ttlSeconds = 300,
    condition = "#orderId != null"
)
```

**성능 최적화:**
- Caffeine 캐시 엔진
- 조건부 캐싱
- TTL 기반 만료

### 6. @RetryOnFailure - 자동 재시도
```java
@RetryOnFailure(
    maxAttempts = 3,
    backoffMillis = 500,
    exponentialBackoff = true,
    retryOn = {RuntimeException.class},
    noRetryOn = {SecurityException.class}
)
```

**안정성 강화:**
- 지수/선형 백오프
- 예외 타입별 제어
- 폴백 메서드 지원

### 7. @ValidateRequest - 입력 검증
```java
@ValidateRequest(
    validateNulls = true,
    validateEmpty = true,
    customValidator = {CustomOrderValidator.class},
    exceptionType = SecurityException.class
)
```

**보안 강화:**
- Bean Validation 통합
- 커스텀 검증기 지원
- 예외 타입 커스터마이징

## 🔄 AOP 실행 순서

Spring `@Order` 어노테이션으로 Aspect 실행 순서를 보장합니다:

```java
@Order(1)  RateLimitAspect        // 요청 제한 (최우선)
@Order(2)  ValidateRequestAspect  // 입력 검증
@Order(3)  IdempotentAspect       // 멱등성 확인
@Order(4)  CacheResultAspect      // 캐시 확인
@Order(5)  ApiLoggingAspect       // API 로깅
@Order(6)  AuditLogAspect         // 감사 로깅
@Order(7)  RetryAspect           // 재시도 로직 (마지막)
```

## 🎯 도메인별 적용 전략

### Order 도메인
- **주문 생성**: 강력한 Rate Limiting + 멱등성
- **주문 조회**: 캐싱 + 성능 로깅
- **관리 작업**: 감사 로깅 + 보안 검증

### Payment 도메인
- **결제 승인**: 극도로 제한적 Rate Limiting
- **토큰 처리**: 보안 강화 + 캐싱
- **상태 변경**: 완전한 감사 추적

## 📊 성능 및 보안 매트릭스

| 작업 유형 | Rate Limit | 캐시 TTL | 감사 레벨 | 재시도 |
|-----------|------------|----------|-----------|--------|
| 주문 생성 | 10/분 | - | INFO | 3회 |
| 결제 승인 | 3/분 | - | ERROR | 2회 |
| 주문 조회 | 30/분 | 300초 | - | 2회 |
| 관리 작업 | 5/시간 | - | WARN | - |

## 🚀 사용 예시

### 간단한 API 메서드
```java
@GetMapping("/orders/{id}")
@CacheResult(cacheName = "orderCache", keyExpression = "#id", ttlSeconds = 300)
@ApiLogging(message = "주문 조회")
public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
    return ok(orderService.getOrder(id));
}
```

### 복합적 보안이 필요한 메서드
```java
@PostMapping
@RateLimit(requests = 10, window = 60, keyExpression = "#authentication.principal.userId")
@ApiLogging(message = "주문 생성", maskSensitiveData = true)
@ValidateRequest(validateNulls = true, validateEmpty = true)
@AuditLog(action = "ORDER_CREATE", resource = "ORDER")
@Idempotent(keyExpression = "#authentication.principal.userId + ':create:' + T(java.time.LocalDate).now()")
@RetryOnFailure(maxAttempts = 3, exponentialBackoff = true)
public ResponseEntity<CreateOrderResponse> createOrder(
    @Valid @RequestBody CreateOrderRequest request,
    Authentication authentication) {
    return ok(orderService.createOrder(request, authentication));
}
```

## 🔧 설정 및 의존성

### Gradle 의존성
```gradle
implementation 'org.springframework.boot:spring-boot-starter-aop'
implementation 'com.github.ben-manes.caffeine:caffeine'
implementation 'org.springframework.boot:spring-boot-starter-validation'
```

### 프로퍼티 설정
```yaml
logging:
  level:
    com.popcorn.demo.common.aop: DEBUG
    AUDIT: INFO
```

## 🏆 주요 성과

### ✅ 완료된 작업
- **7개 핵심 어노테이션** 구현 및 테스트
- **Order 도메인** 전체 컨트롤러 적용
- **Payment 도메인** 전체 컨트롤러 적용
- **테스트 코드** 정상 컴파일 완료

### 📈 기대 효과
- **보안 강화**: Rate Limiting으로 DDoS 방어
- **성능 향상**: 캐싱으로 응답 속도 개선
- **운영 효율성**: 자동화된 로깅 및 감사
- **개발 생산성**: 선언적 프로그래밍으로 코드 간소화

## 🔮 향후 확장 계획

1. **Redis 캐시** 지원 추가
2. **Micrometer 메트릭** 통합
3. **분산 Rate Limiting** 구현
4. **추가 도메인** 적용 (User, Store 등)

---

*📝 작성일: 2026-01-15*
*🔧 작성자: AOP 어노테이션 시스템 구축팀*