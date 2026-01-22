package com.popcorn.common.config.flyway;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "common.flyway.schema", name = "enabled", havingValue = "true", matchIfMissing = false)

public class FlywaySchemaConfig {



	@Bean(name = "flywaySchema")
	public Flyway flywaySchema(DataSource dataSource, ObjectProvider<FlywayProperties> propertiesProvider) {
		FlywayProperties properties = propertiesProvider.getIfAvailable();
		FluentConfiguration configuration = Flyway.configure()
			.dataSource(dataSource);

		if (properties == null) {
			configuration
				.locations("classpath:db/migration/schema", "classpath:db/migration/seed")
				.table("flyway_schema_history")
				.baselineOnMigrate(true)
				.outOfOrder(false)
				.validateOnMigrate(false);
		} else {
			if (properties.getLocations() == null || properties.getLocations().isEmpty()) {
				configuration.locations("classpath:db/migration/schema", "classpath:db/migration/seed");
			} else {
				configuration.locations(properties.getLocations().toArray(new String[0]));
			}

			if (properties.getSchemas() != null && !properties.getSchemas().isEmpty()) {
				configuration.schemas(properties.getSchemas().toArray(new String[0]));
			}

			if (properties.getDefaultSchema() != null && !properties.getDefaultSchema().isBlank()) {
				configuration.defaultSchema(properties.getDefaultSchema());
			}

			if (properties.getTable() != null && !properties.getTable().isBlank()) {
				configuration.table(properties.getTable());
			}

			configuration
				.baselineOnMigrate(properties.isBaselineOnMigrate())
				.outOfOrder(properties.isOutOfOrder())
				.validateOnMigrate(properties.isValidateOnMigrate())
				.createSchemas(properties.isCreateSchemas());
		}

		Flyway flyway = configuration.load();
		flyway.migrate();
		return flyway;
	}

}
