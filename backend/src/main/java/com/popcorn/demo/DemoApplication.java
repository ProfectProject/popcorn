package com.popcorn.demo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories; 

@SpringBootApplication
@ComponentScan(basePackages = {"com.popcorn.demo", "com.popcorn.common"})
@EnableJpaRepositories(basePackages = "com.popcorn.demo")
@EntityScan(basePackages = "com.popcorn.demo") 
public class DemoApplication 
{ 
	public static void main(String[] args) 
	{ SpringApplication.run(DemoApplication.class, args); } 
}
