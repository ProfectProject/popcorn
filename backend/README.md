# 🍿 Popcorn Backend - 폴더 구조

## 📂 DDD Clean Architecture 폴더 구조 ✅

```
src/main/java/com/ttalkak/demo/
├── domain/                      # 도메인 레이어 (순수 Java, Spring 의존성 없음)
│   ├── user/
│   │   ├── entity/             # 사용자 엔티티
│   │   ├── vo/                 # 값 객체 (Value Object)
│   │   ├── repository/         # 레포지토리 인터페이스
│   │   └── service/            # 도메인 서비스
│   ├── order/
│   │   ├── entity/
│   │   ├── vo/
│   │   ├── repository/
│   │   └── service/
│   ├── store/
│   │   ├── entity/
│   │   ├── vo/
│   │   ├── repository/
│   │   └── service/
│   └── product/
│       ├── entity/
│       ├── vo/
│       ├── repository/
│       └── service/
│
├── application/                 # 애플리케이션 레이어
│   ├── usecase/                # 유스케이스
│   ├── service/                # 애플리케이션 서비스
│   ├── dto/                    # 애플리케이션 DTO
│   └── config/                 # DI 설정 ✅
│
├── infrastructure/              # 인프라스트럭처 레이어
│   ├── persistence/            # 데이터베이스
│   │   ├── entity/             # JPA 엔티티
│   │   ├── repository/         # JPA 레포지토리 구현
│   │   └── mapper/             # 도메인 ↔ JPA 매퍼
│   ├── external/               # 외부 서비스
│   └── config/                 # DI 설정 ✅
│
├── presentation/                # 프레젠테이션 레이어
│   ├── web/                    # 웹 컨트롤러
│   │   ├── controller/         # REST 컨트롤러
│   │   └── dto/                # 웹 DTO (Request/Response)
│   └── config/                 # DI 설정 ✅
│
└── DemoApplication.java         # Spring Boot 메인 클래스 ✅
```