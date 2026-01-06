package com.popcorn.demo.global.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("local")
public class LocalSeedRunner implements ApplicationRunner {

	@Override
	public void run(ApplicationArguments args) {
		log.info("LocalSeedRunner skipped: V0 schema seed is handled by Flyway seed files.");
	}
}
