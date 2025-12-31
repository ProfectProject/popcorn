package com.popcorn.demo.common.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;

import io.r2dbc.spi.ConnectionFactory;

/**
 * Transaction Manager 설정
 * R2DBC와 JDBC Transaction Manager 충돌 해결을 위해 각각 명시적으로 정의
 */
@Configuration
public class TransactionManagerConfig {

	/**
	 * R2DBC Transaction Manager
	 * 리액티브 데이터베이스 작업용
	 */
	@Bean
	@Primary
	public ReactiveTransactionManager reactiveTransactionManager(ConnectionFactory connectionFactory) {
		return new R2dbcTransactionManager(connectionFactory);
	}

	/**
	 * JDBC Transaction Manager
	 * JDBC Template과 Flyway 등의 동기 작업용
	 */
	@Bean
	public PlatformTransactionManager jdbcTransactionManager(DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}
}