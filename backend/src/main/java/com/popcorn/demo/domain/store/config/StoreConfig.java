package com.popcorn.demo.domain.store.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@ComponentScan(basePackages = {
        "com.popcorn.demo.domain.store.service",
		"com.popcorn.demo.domain.store.repository",
		"com.popcorn.demo.domain.store.controller"})
public class StoreConfig {

}
