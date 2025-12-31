package com.popcorn.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = OrderTestApplication.class)
@ActiveProfiles("test")
class DemoApplicationTests {


	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
		assertThat(applicationContext).isNotNull();
		assertThat(applicationContext.containsBean("requestLoggingInterceptor")).isTrue();
		assertThat(applicationContext.containsBean("objectMapper")).isTrue();
	}
}
