package com.popcorn.demo.order;

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
public class OrderApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderApplication.class, args);
	}
}
