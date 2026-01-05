# 🍿 Popcorn Backend

## 📋 프로젝트 개요

Popcorn Backend는 **Spring Boot 3.5.9**, **Spring WebMVC**, **JPA** 기반의 주문/예약 시스템입니다.
도메인 중심 패키지 구조와 공통 모듈 분리를 통해 유지보수성과 확장성을 높였습니다.

## 📂 패키지 구조 (Domain Layer 방식)

```
src/main/java/com/popcorn/demo/
├── DemoApplication.java
├── domain/                          # 도메인별 기능
│   ├── user/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   └── dto/
│   └── order/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── entity/
│       ├── dto/
│       ├── event/
│       └── exception/
│   └── popup/
│       ├── application/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── entity/
│       ├── dto/
│       ├── event/
│       └── exception/
├── global/                          # 전역 공통 모듈
│   ├── config/
│   ├── exception/
│   ├── filter/
│   └── util/
└── common/                          # 도메인 공통 모듈
    ├── dto/
    ├── entity/
    ├── controller/
    └── cache/
```

## 🛠️ 기술 스택

- **Language**: Java 17
- **Framework**: Spring Boot 3.5.9 (WebMVC)
- **ORM**: Spring Data JPA
- **Database**: PostgreSQL 18.1
- **Migration**: Flyway
- **Cache**: Caffeine
- **API Docs**: SpringDoc OpenAPI
- **Build**: Gradle

## ⚙️ 환경 변수

로컬 실행을 위해 `.env` 파일을 사용합니다.

```
backend/.env
backend/.env.example
```

필수 변수:
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`

GitHub Actions에서는 동일한 이름의 **Repository Secrets**를 사용합니다.

## ▶️ 실행 방법

```bash
cd backend
./gradlew bootRun
```

## 📡 API 문서

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
