package com.popcorn.demo;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.springframework.context.annotation.Import;

import com.popcorn.demo.config.TestRedisConfig;
import com.popcorn.demo.config.TestSecurityConfig;

@Suite
@SelectPackages("com.popcorn.demo.domain.store")
@Import({TestSecurityConfig.class, TestRedisConfig.class})
class StoreApplicationTests {
}
