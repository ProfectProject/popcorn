package com.popcorn.demo.application.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**

	* Application Layer DI 설정

	*/

@Configuration

@ComponentScan(basePackages = {
	"com.popcorn.demo.application"
})

public class ApplicationConfig {

	// Use Case와 Application Service Bean 스캔

}
