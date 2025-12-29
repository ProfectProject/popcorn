package com.ttalkak.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
	"com.ttalkak.demo.application",     // Application Layer
	"com.ttalkak.demo.infrastructure",  // Infrastructure Layer
	"com.ttalkak.demo.presentation"     // Presentation Layer
})
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
