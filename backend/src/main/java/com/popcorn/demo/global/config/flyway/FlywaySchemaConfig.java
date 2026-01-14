package com.popcorn.demo.global.config.flyway;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)

public class FlywaySchemaConfig {



	@Bean(name = "flywaySchema")

	public Flyway flywaySchema(DataSource dataSource) {

		Flyway flyway = Flyway.configure()

				.dataSource(dataSource)

				.locations("classpath:db/migration/schema", "classpath:db/migration/seed")

				.table("flyway_schema_history")

				.baselineOnMigrate(true)

				.outOfOrder(false)

				.validateOnMigrate(false)

				.load();



		flyway.migrate();

		return flyway;

	}

}

