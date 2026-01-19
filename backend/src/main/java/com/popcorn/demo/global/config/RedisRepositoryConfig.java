package com.popcorn.demo.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories(basePackages = "com.popcorn.demo.domain.redis")
public class RedisRepositoryConfig {
}
