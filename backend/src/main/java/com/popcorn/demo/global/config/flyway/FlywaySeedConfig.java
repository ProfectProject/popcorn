package com.popcorn.demo.global.config.flyway;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;

@Configuration

@Profile({ "local", "dev" })

public class FlywaySeedConfig {



	@Bean

	@DependsOn("flywaySchema")

	public Flyway flywaySeed(DataSource dataSource) {

		Flyway flyway = Flyway.configure()

				.dataSource(dataSource)

				.locations("classpath:db/migration/seed", "classpath:db/migration/seed/qr")

				.table("flyway_seed_qr_history")

				.baselineOnMigrate(true)

				.outOfOrder(false)

				.load();



		flyway.repair();
		flyway.migrate();

		return flyway;

	}

}
