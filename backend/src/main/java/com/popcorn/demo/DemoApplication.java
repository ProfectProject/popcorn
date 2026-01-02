package com.popcorn.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
		scanBasePackages = {
				"com.popcorn.demo.application.order",
				"com.popcorn.demo.domain.order",
				"com.popcorn.demo.infrastructure",
				"com.popcorn.demo.common"
		}
)
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}
