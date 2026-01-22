package com.popcorn.payment.config

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.TestPropertySource

/**
 * 테스트용 H2 데이터베이스 설정
 * PostgreSQL 스키마 의존성을 제거하고 H2 호환성을 확보
 */
@Configuration
@TestPropertySource(properties = [
    "spring.jpa.properties.hibernate.default_schema=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
])
class TestEntityConfiguration