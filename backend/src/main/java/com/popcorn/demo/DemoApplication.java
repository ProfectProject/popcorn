package com.popcorn.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {
	"com.popcorn.demo.domain.order.repository.jpa",
	"com.popcorn.demo.domain.store.repository.jpa",
	"com.popcorn.demo.domain.popup.repository"
})
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}
