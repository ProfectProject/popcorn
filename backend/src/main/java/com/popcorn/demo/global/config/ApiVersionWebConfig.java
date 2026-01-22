package com.popcorn.demo.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.popcorn.common.versioning.ApiVersionInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ApiVersionWebConfig implements WebMvcConfigurer {

	private final ApiVersionInterceptor apiVersionInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(apiVersionInterceptor)
				.addPathPatterns("/api/**");
	}
}
