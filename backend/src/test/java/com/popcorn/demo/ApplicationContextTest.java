package com.popcorn.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootTest(
	classes = ApplicationContextTest.TestConfig.class,
	properties = {
		"spring.flyway.enabled=false",
		"spring.profiles.active=test"
	}
)
class ApplicationContextTest {

	@SpringBootConfiguration
	@ComponentScan(basePackages = {
		"com.popcorn.demo.common",
		"com.popcorn.demo.presentation"
	})
	@Import(com.popcorn.demo.presentation.config.PresentationConfig.class)
	static class TestConfig {
	}

	@Test
	void contextLoads() {
	}
}
