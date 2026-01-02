package com.popcorn.demo.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration

@ComponentScan(basePackages = "com.popcorn.demo.common")

public class CommonConfig {

	// 공통 컴포넌트 및 빈 설정
	@Bean
	public ObjectMapper objectMapper() {
		// 날짜/시간은 ISO 문자열로 직렬화하고, 기본 타임존 보정은 끕니다.
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		mapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
		return mapper;
	}

}
