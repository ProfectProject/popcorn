# 🍿 Popcorn Backend


## 📋 프로젝트 개요

팝콘은 **DDD(Domain Driven Design)** 와 **Clean Architecture** 를 기반으로 구축된 Spring Boot 애플리케이션입니다.
주문/예약 시스템을 제공하는 백엔드 서비스로, 확장 가능하고 유지보수가 용이한 구조를 목표로 합니다.

## 🏗️ 아키텍처 개요

### Clean Architecture + DDD

```
┌─────────────────────────────────────────┐
│             External Systems            │ ← 외부 시스템
├─────────────────────────────────────────┤
│          Infrastructure Layer           │ ← 인프라스트럭처 레이어
│  • JPA Repository Implementations       │   (프레임워크, 드라이버, DB)
│  • External API Clients                 │
│  • Database Configurations              │
├─────────────────────────────────────────┤
│           Application Layer             │ ← 애플리케이션 레이어
│  • Use Cases                            │   (비즈니스 플로우 조정)
│  • Application Services                 │
│  • DTO Transformations                  │
├─────────────────────────────────────────┤
│             Domain Layer                │ ← 도메인 레이어 (핵심)
│  • Entities                             │   (순수 비즈니스 로직)
│  • Domain Services                      │
│  • Repository Interfaces                │
│  • Business Rules                       │
└─────────────────────────────────────────┘
```

### 의존성 방향 규칙

- **의존성은 안쪽(Domain)을 향해야 함** ⬇️
- **Domain Layer는 외부 레이어를 알지 못함** 🚫
- **인터페이스를 통한 의존성 역전** 🔄

## 📂 폴더 구조

```
src/main/java/com/ttalkak/demo/
├── domain/                      # 도메인 레이어
│   ├── user/
│   │   ├── controller/         # 사용자 REST 컨트롤러
│   │   ├── dto/                # 사용자 DTO
│   │   ├── entity/             # 사용자 엔티티
│   │   ├── repository/         # 레포지토리 인터페이스
│   │   └── service/            # 도메인 서비스
│   ├── order/
│   │   ├── controller/         # 주문 REST 컨트롤러
│   │   ├── dto/                # 주문 DTO
│   │   ├── entity/
│   │   ├── repository/
│   │   └── service/
│   ├── store/
│   │   ├── controller/         # 스토어 REST 컨트롤러
│   │   ├── dto/                # 스토어 DTO
│   │   ├── entity/
│   │   ├── repository/
│   │   └── service/
│   └── product/
│       ├── controller/         # 상품 REST 컨트롤러
│       ├── dto/                # 상품 DTO
│       ├── entity/
│       ├── repository/
│       └── service/
│
├── application/                 # 애플리케이션 레이어
│   ├── usecase/                # 유스케이스
│   ├── service/                # 애플리케이션 서비스
│   ├── dto/                    # 애플리케이션 DTO
│   └── config/                 # DI 설정
│
├── infrastructure/              # 인프라스트럭처 레이어
│   ├── persistence/            # 데이터베이스
│   │   ├── entity/             # JPA 엔티티
│   │   ├── repository/         # JPA 레포지토리 구현
│   │   └── mapper/             # 도메인 ↔ JPA 매퍼
│   ├── external/               # 외부 서비스
│   └── config/                 # 인프라 설정
│
└── DemoApplication.java         # Spring Boot 메인 클래스
```

## 🛠️ 기술 스택

### 핵심 기술
- **Language**: Java 17
- **Framework**: Spring Boot 3.5.9
- **Build Tool**: Gradle 8.14.3
- **Database**: PostgreSQL
- **ORM**: Spring Data JPA
- **Migration**: Flyway

### Spring Boot Dependencies
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.postgresql:postgresql'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

### 개발 환경
- **IDE**: IntelliJ IDEA / VS Code
- **Java Version**: 17 (LTS)
- **Container**: Docker (PostgreSQL)
- **Version Control**: Git

## ⚙️ 실행 방법

### 1. 사전 준비
```bash
# Java 17 확인
java -version

# Docker로 PostgreSQL 실행
docker run --name popcorn-postgres \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=1234 \
  -e POSTGRES_DB=ttalkak_db \
  -p 5432:5432 \
  -d postgres:15
```

### 2. 애플리케이션 실행
```bash
# 로컬 환경에서 실행
./gradlew bootRun --args='--spring.profiles.active=local'

# 개발 환경에서 실행
./gradlew bootRun --args='--spring.profiles.active=dev'

# 빌드
./gradlew build
```

### 3. 애플리케이션 접속
- **서버**: http://localhost:8080
- **H2 Console** (개발용): http://localhost:8080/h2-console

## 🔧 레이어별 책임

| 레이어 | 책임 | 주요 컴포넌트 | Spring 어노테이션 |
|--------|------|---------------|-------------------|
| **Domain** | 비즈니스 규칙, 엔티티, 도메인 로직 | Entity, Repository Interface | `@Component` (컨트롤러용) |
| **Application** | 유스케이스 조정, 트랜잭션 관리 | UseCase, Application Service | `@Service`, `@Component` |
| **Infrastructure** | 데이터 영속성, 외부 서비스 연동 | JPA Repository, External Client | `@Repository`, `@Component` |

## 🏛️ DDD 패턴

### Aggregate (애그리게이트)
- **Order**: 주문 관련 엔티티들의 일관성 경계
- **User**: 사용자 관련 엔티티들의 일관성 경계
- **Store**: 스토어 관련 엔티티들의 일관성 경계
- **Product**: 상품 관련 엔티티들의 일관성 경계


### Repository Pattern
- 도메인 레이어: 인터페이스 정의
- 인프라 레이어: JPA 구현체 제공

## 📊 환경별 설정

### Local (개발자 로컬)
- **Database**: PostgreSQL (localhost:5432)
- **Profile**: `local`
- **DDL**: `none` (Flyway 관리)

### Dev (개발 서버)
- **Database**: 환경변수로 설정
- **Profile**: `dev`
- **DDL**: `validate`

### Prod (운영 서버)
- **Database**: 환경변수로 설정
- **Profile**: `prod`
- **DDL**: `validate`

