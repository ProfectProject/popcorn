package com.popcorn.demo.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication(
		scanBasePackages = {
				"com.popcorn.demo.application.order",
				"com.popcorn.demo.domain.order",
				"com.popcorn.demo.infrastructure",
				"com.popcorn.demo.common"
		}
)
@EntityScan(basePackages = "com.popcorn.demo.domain.order.entity")
public class OrderApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderApplication.class, args);
	}
}
