package com.popcorn.common.config.flyway;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FlywayProperties.class)
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)

public class FlywaySchemaConfig {



	@Bean(name = "flywaySchema")

	public Flyway flywaySchema(DataSource dataSource, FlywayProperties properties) {

		FluentConfiguration configuration = Flyway.configure()

				.dataSource(dataSource)

				.baselineOnMigrate(properties.isBaselineOnMigrate())

				.outOfOrder(properties.isOutOfOrder())

				.validateOnMigrate(properties.isValidateOnMigrate())

				.cleanDisabled(properties.isCleanDisabled())

				.createSchemas(properties.isCreateSchemas());

		if (properties.getLocations() != null && !properties.getLocations().isEmpty()) {
			configuration.locations(properties.getLocations().toArray(new String[0]));
		}
		if (properties.getSchemas() != null && !properties.getSchemas().isEmpty()) {
			configuration.schemas(properties.getSchemas().toArray(new String[0]));
		}
		if (properties.getTable() != null && !properties.getTable().isEmpty()) {
			configuration.table(properties.getTable());
		}
		if (properties.getPlaceholders() != null && !properties.getPlaceholders().isEmpty()) {
			configuration.placeholders(properties.getPlaceholders());
		}

		Flyway flyway = configuration.load();



		flyway.migrate();

		return flyway;

	}

}
