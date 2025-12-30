package com.popcorn.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.popcorn.demo.application.config.ApplicationConfig;
import com.popcorn.demo.common.config.CommonConfig;
import com.popcorn.demo.infrastructure.config.InfrastructureConfig;
import com.popcorn.demo.presentation.config.PresentationConfig;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({
	CommonConfig.class,
	ApplicationConfig.class,
	InfrastructureConfig.class,
	PresentationConfig.class
})
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
