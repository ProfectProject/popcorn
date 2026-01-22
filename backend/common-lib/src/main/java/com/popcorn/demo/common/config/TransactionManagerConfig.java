package com.popcorn.demo.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;

import jakarta.persistence.EntityManagerFactory;

/**
 * Transaction Manager 설정
 * JPA Transaction Manager를 명시적으로 정의
 */
@Configuration
public class TransactionManagerConfig {

	/**
	 * JPA Transaction Manager
	 * JPA/JDBC/Flyway 같은 블로킹 작업용 트랜잭션 매니저입니다.
	 */
	@Bean(name = {"jdbcTransactionManager", "transactionManager"})
	@Primary
	public PlatformTransactionManager jdbcTransactionManager(EntityManagerFactory entityManagerFactory) {
		return new JpaTransactionManager(entityManagerFactory);
	}
}