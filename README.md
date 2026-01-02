# 🍿 Popcorn Backend

## 📋 프로젝트 개요

팝콘은 **Spring Boot 3.5.9**, **WebFlux**, **R2DBC**를 기반으로 한 **리액티브 주문/예약 시스템**입니다.
**DDD(Domain Driven Design)** 와 **Clean Architecture** 원칙을 따르는 고도로 구조화된 백엔드 애플리케이션입니다.

### 주요 특징
- 🚀 **리액티브 프로그래밍**: WebFlux + R2DBC 기반 비동기 처리
- 🏗️ **Clean Architecture**: 명확한 계층 분리와 의존성 규칙 준수
- 🎯 **DDD 패턴**: Aggregate, Entity, Enum 적용
- 🔌 **Port & Adapter**: 기술 독립적인 비즈니스 로직 구현
- 📚 **API 문서화**: Swagger/OpenAPI 자동 생성
- 🧪 **포괄적 테스트**: 단위/통합 테스트 완비

## 🏗️ Clean Architecture + DDD

```
┌─────────────────────────────────────────────┐
│      Presentation Layer (Controllers)       │ ← HTTP 요청/응답 (@RestController)
├─────────────────────────────────────────────┤
│      Application Layer (Use Cases)          │ ← 비즈니스 흐름 조정 (@Service)
│  • CreateOrderUseCase                       │
│  • UpdateOrderStatusUseCase                 │
│  • Port Interfaces (Input/Output)           │
├─────────────────────────────────────────────┤
│      Domain Layer (핵심 비즈니스)            │ ← 순수 도메인 로직
│  • Entities (Order, OrderItem)              │
│  • Domain Services                          │
│  • Repository Interfaces                    │
│  • Enums & Domain Objects                   │
├─────────────────────────────────────────────┤
│      Infrastructure Layer                   │ ← 기술 구현 (@Repository)
│  • R2DBC Repositories                       │
│  • Adapters (Port 구현체)                   │
│  • External Service Clients                 │
└─────────────────────────────────────────────┘
```

### 의존성 방향 규칙
- **의존성은 안쪽(Domain)을 향해야 함** ⬇️
- **Domain Layer는 외부 레이어를 알지 못함** 🚫
- **Port & Adapter로 의존성 역전** 🔄

## 📂 실제 패키지 구조

```
src/main/java/com/popcorn/demo/
├── domain/order/                    # 도메인 레이어
│   ├── controller/                  # REST 컨트롤러
│   │   ├── OrderController.java     # @RestController
│   │   ├── OrderExceptionHandler.java  # @RestControllerAdvice
│   │   └── OrderTestController.java
│   ├── entity/                      # 도메인 엔티티
│   │   ├── Order.java              # @Table("p_orders")
│   │   ├── OrderItem.java          # @Table("p_order_items")
│   │   ├── OrderStatusHistory.java
│   │   ├── OrderStatus.java        # Enum (9개 상태)
│   │   ├── OrderType.java          # Enum: RESERVATION, PURCHASE
│   │   └── OrderItemType.java      # Enum: RESERVATION, MERCH
│   ├── dto/                        # 요청/응답 DTO
│   │   ├── CreateOrderRequest.java
│   │   ├── UpdateOrderStatusRequest.java
│   │   └── OrderCreatedDto.java
│   ├── repository/                 # Repository 인터페이스
│   │   └── OrderRepository.java    # (20+ 메서드)
│   ├── service/                    # 도메인 서비스
│   │   ├── OrderService.java       # @Service
│   │   └── OrderDomainService.java
│   └── exception/
│       └── OrderException.java
│
├── application/order/               # 애플리케이션 레이어
│   ├── usecase/                    # 유스케이스 (@Service)
│   │   ├── CreateOrderUseCase.java
│   │   └── UpdateOrderStatusUseCase.java
│   ├── port/                       # Port 인터페이스
│   │   ├── in/                     # Input Port
│   │   │   ├── CreateOrderCommand.java
│   │   │   └── CreateOrderResponse.java
│   │   └── out/                    # Output Port
│   │       ├── SaveOrderPort.java
│   │       ├── FindOrderPort.java
│   │       ├── ProcessOrderPort.java
│   │       └── NotifyOrderPort.java
│   └── event/                      # 도메인 이벤트
│       ├── OrderCreatedEvent.java
│       └── OrderPostProcessingListener.java
│
├── infrastructure/                  # 인프라스트럭처 레이어
│   ├── persistence/
│   │   ├── order/
│   │   │   ├── OrderRepositoryAdapter.java  # Port 구현체
│   │   │   └── OrderItemPriceAdapter.java
│   │   └── repository/
│   │       ├── OrderRepositoryImpl.java     # @Repository
│   │       ├── R2dbcOrderRepository.java    # extends ReactiveCrudRepository
│   │       ├── R2dbcOrderItemRepository.java
│   │       └── R2dbcOrderStatusHistoryRepository.java
│   ├── external/
│   │   ├── async/OrderAdapter.java
│   │   └── notification/OrderNotificationAdapter.java
│   └── config/InfrastructureConfig.java
│
└── common/                          # 공통 레이어
    ├── config/                      # 설정 클래스
    │   ├── SecurityConfig.java      # Spring Security
    │   ├── TransactionManagerConfig.java
    │   ├── DataSourceConfig.java
    │   ├── OpenApiConfig.java       # Swagger 설정
    │   └── LocalSeedRunner.java
    ├── dto/
    │   ├── BaseResponse.java        # 공통 응답 래퍼
    │   └── ResponseCode.java        # 응답 코드 Enum (20+ 항목)
    ├── exception/BaseException.java
    ├── entity/BaseEntity.java       # 공통 엔티티
    ├── filter/RequestTraceFilter.java
    └── cache/IdempotencyCache.java   # Caffeine 캐시
```

## 🛠️ 기술 스택

### 핵심 기술
- **Language**: Java 17 LTS
- **Framework**: Spring Boot 3.5.9
- **Reactive**: Spring WebFlux (비동기 웹)
- **Database**: PostgreSQL 18.1
- **ORM**: Spring Data R2DBC (리액티브)
- **Migration**: Flyway
- **Cache**: Caffeine 3.1.8
- **Security**: Spring Security (WebFlux)
- **API Docs**: SpringDoc OpenAPI 2.8.0
- **Build Tool**: Gradle 8.14.3

### 주요 의존성
```gradle
dependencies {
    // 리액티브 웹 & 데이터
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'org.springframework.boot:spring-boot-starter-data-r2dbc'
    implementation 'org.postgresql:r2dbc-postgresql'

    // 보안 & 검증
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // API 문서화
    implementation 'org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.0'

    // 데이터베이스 마이그레이션
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-database-postgresql'

    // 캐싱
    implementation 'org.springframework.boot:spring-boot-starter-cache'
    implementation 'com.github.ben-manes.caffeine:caffeine:3.1.8'

    // 유틸리티
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // 테스트
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'io.r2dbc:r2dbc-h2'
    testImplementation 'io.projectreactor:reactor-test'
}
```

### 개발 환경
- **IDE**: IntelliJ IDEA / VS Code
- **Java Version**: 17 (LTS)
- **Container**: Docker (PostgreSQL)
- **Version Control**: Git
- **Code Style**: Naver Checkstyle 규칙

## 📡 REST API

### 주요 엔드포인트

| Method | URL | 설명 | 요청/응답 |
|--------|-----|------|----------|
| `POST` | `/api/v1/orders/{userId}` | 주문 생성 | CreateOrderRequest → OrderCreatedDto |
| `PATCH` | `/api/v1/orders/{orderId}/status` | 주문 상태 변경 | UpdateOrderStatusRequest → UpdateOrderStatusResponse |

### API 문서화
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **자동 생성**: SpringDoc OpenAPI 2.8.0

> 📚 모든 API 상세 정보, 요청/응답 예시, 스키마는 Swagger UI에서 확인하세요.

## 🗄️ 데이터베이스

### 데이터베이스 정보
- **DBMS**: PostgreSQL 18.1
- **주요 테이블**: `p_orders`, `p_order_items`, `p_order_status_histories`
- **마이그레이션**: Flyway 자동 관리

#### 상태 전이 다이어그램
```
REQUESTED
  ↓
OWNER_ACCEPTED ────→ OWNER_REJECTED
  ↓
CONFIRMED
  ↓
PREPARING
  ↓
READY
  ↓
COMPLETED

(언제든지) ↘ CANCELLED ↙ REFUNDED
```

### Flyway 마이그레이션
```
db/migration/
├── schema/                      # 스키마 정의
│   ├── V1__enable_uuid.sql
│   ├── V2__enum_types.sql
│   ├── V8__create_p_orders_table.sql
│   └── ...
└── seed/                        # 테스트 데이터
    ├── V1__seed_local_order_data.sql
    └── ...
```

> 📋 전체 스키마 정보는 마이그레이션 파일을 참고하세요.

## 🔧 레이어별 책임

| 레이어 | 책임 | 주요 컴포넌트 | Spring 어노테이션 |
|--------|------|---------------|-------------------|
| **Presentation** | HTTP 요청/응답 처리 | Controller, ExceptionHandler | `@RestController`, `@RestControllerAdvice` |
| **Application** | 유스케이스 조정, 트랜잭션 관리 | UseCase, Port Interface | `@Service`, `@Transactional` |
| **Domain** | 비즈니스 규칙, 엔티티, 도메인 로직 | Entity, Domain Service, Repository Interface | `@Service` (Domain Service) |
| **Infrastructure** | 데이터 영속성, 외부 서비스 연동 | R2DBC Repository, Adapter | `@Repository`, `@Component` |

## 🏛️ DDD 패턴 구현

### Aggregate (주문 애그리게이트)
```java
@Table("p_orders")
public class Order extends BaseEntity {
    @Id private UUID id;
    private String orderNo;
    private Long customerId;
    private OrderStatus status;
    private Integer totalAmount;

    @Transient
    private List<OrderItem> orderItems = new ArrayList<>();

    // 도메인 메서드
    public boolean isCancelable() { ... }
    public int getTotalQuantity() { ... }
    public void addOrderItems(List<OrderItem> items) { ... }
}
```

### Repository Pattern
- **도메인 레이어**: Repository 인터페이스 정의
- **인프라 레이어**: R2DBC 구현체 제공
```java
// 도메인
public interface OrderRepository {
    Mono<Order> findById(UUID id);
    Mono<Order> save(Order order);
}

// 인프라
@Repository
public class OrderRepositoryAdapter implements OrderRepository {
    private final R2dbcOrderRepository r2dbcRepository;
    // ...
}
```

### Port & Adapter 패턴
```java
// Output Port (애플리케이션 → 인프라)
public interface SaveOrderPort {
    Mono<Order> save(Order order);
    Mono<Void> saveOrderItems(List<OrderItem> items);
}

// Adapter (포트 구현체)
@Component
public class OrderRepositoryAdapter implements SaveOrderPort {
    // 실제 R2DBC 구현
}
```

## 🧪 테스트 전략

### 테스트 구조
```
src/test/java/com/popcorn/demo/
├── application/order/usecase/
│   ├── CreateOrderUseCaseTest.java        # Use Case 테스트
│   └── UpdateOrderStatusUseCaseTest.java
├── domain/order/
│   ├── controller/OrderControllerTest.java # 컨트롤러 테스트
│   ├── entity/OrderTest.java               # 엔티티 테스트
│   └── service/OrderDomainServiceTest.java # 도메인 서비스 테스트
├── infrastructure/
│   ├── persistence/order/OrderRepositoryAdapterTest.java
│   └── persistence/repository/OrderRepositoryImplTest.java
└── resources/application-test.yml
```

### 테스트 실행
```bash
# 전체 테스트
./gradlew test

# 특정 테스트 클래스
./gradlew test --tests CreateOrderUseCaseTest

# 코드 커버리지와 함께
./gradlew test jacocoTestReport
```

## 📊 환경별 설정

### Local (개발자 로컬)
```yaml
spring:
  profiles:
    active: local
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/popcorn_db
    username: postgres
    password: 1234
  security:
    enabled: false  # 개발 편의
logging:
  level:
    com.popcorn.demo: DEBUG
```

### Dev (개발 서버)
```yaml
spring:
  config:
    activate:
      on-profile: dev
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

### Prod (운영 서버)
```yaml
spring:
  config:
    activate:
      on-profile: prod
  r2dbc:
    url: ${DATABASE_URL}
logging:
  level:
    com.popcorn.demo: WARN
```

## ⚙️ 실행 방법

### 1. 사전 준비
```bash
# Java 17 확인
java -version

# Docker로 PostgreSQL 실행
docker run --name popcorn-postgres \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=1234 \
  -e POSTGRES_DB=popcorn_db \
  -p 5432:5432 \
  -d postgres:18
```

### 2. 애플리케이션 실행
```bash
# 로컬 환경에서 실행
./gradlew bootRun --args='--spring.profiles.active=local'

# 개발 환경에서 실행
./gradlew bootRun --args='--spring.profiles.active=dev'

# 빌드
./gradlew build

# 코드 스타일 검사
./gradlew checkstyleMain checkstyleTest
```

### 3. 애플리케이션 접속
- **API 서버**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **헬스 체크**: http://localhost:8080/actuator/health

## 🔄 특수 기능

### 멱등성 지원
```bash
# Idempotency-Key 헤더로 중복 요청 방지
POST /api/v1/orders/1001
Idempotency-Key: order-12345
```

### 이벤트 기반 처리
- 주문 생성 후 자동으로 OrderCreatedEvent 발행
- 비동기 후처리 (재고 차감, 결제, 알림)

### 상태 전이 관리
- OrderDomainService에서 비즈니스 규칙 검증
- 허용되지 않은 상태 전이 차단

### 낙관적 락
- Order 엔티티에 version 필드로 동시성 제어

## 🚀 배포

### Docker 빌드
```bash
# 이미지 빌드
docker build -t popcorn:latest .

# 컨테이너 실행
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=your-db-host \
  -e DB_USERNAME=your-username \
  -e DB_PASSWORD=your-password \
  popcorn:latest
```

### 환경 변수
| 변수 | 설명 | 기본값 |
|------|------|--------|
| `SPRING_PROFILES_ACTIVE` | 활성 프로파일 | local |
| `DB_HOST` | 데이터베이스 호스트 | localhost |
| `DB_PORT` | 데이터베이스 포트 | 5432 |
| `DB_NAME` | 데이터베이스 이름 | popcorn_db |
| `DB_USERNAME` | 데이터베이스 사용자 | postgres |
| `DB_PASSWORD` | 데이터베이스 비밀번호 | 1234 |

## 🎯 아키텍처 특징

### ✅ Clean Architecture 준수
- 의존성 규칙: Domain ← Application ← Infrastructure
- Port & Adapter 패턴으로 기술 독립성 확보
- Use Case를 통한 비즈니스 흐름 명확화

### ✅ DDD 원칙 준수
- Order Aggregate로 비즈니스 무결성 보장
- Domain Service로 복잡한 비즈니스 로직 캡슐화
- Enum으로 OrderStatus, OrderType 모델링

### ✅ 리액티브 프로그래밍
- WebFlux 기반 비동기 요청 처리
- R2DBC를 통한 논블로킹 DB 접근
- Reactor의 Mono/Flux 활용

### ✅ 엔터프라이즈 준비
- 포괄적인 테스트 커버리지
- 다중 환경 설정 지원
- Docker/CI/CD 준비 완료
- 상세한 API 문서화

---

**Popcorn Backend**는 현대적인 백엔드 개발의 모범 사례를 적용한 확장 가능하고 유지보수가 용이한 엔터프라이즈급 애플리케이션입니다.