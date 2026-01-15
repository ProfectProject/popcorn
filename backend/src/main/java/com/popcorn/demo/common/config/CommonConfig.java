package com.popcorn.demo.common.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.popcorn.demo.domain.auth.dto.CustomUserDetails;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@ComponentScan(basePackages = {
		"com.popcorn.demo.global",
		"com.popcorn.demo.common"
})
public class CommonConfig {

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		mapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
		return mapper;
	}

	@Bean
	public AuditorAware<Long> auditorAware() {
		return () -> {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

			if (authentication == null || !authentication.isAuthenticated()) {
				return Optional.empty();
			}

			// AnonymousAuthenticationToken인 경우 무시
			if (authentication.getPrincipal() instanceof String && "anonymousUser".equals(authentication.getPrincipal())) {
				return Optional.empty();
			}

			// JWT 필터에서 설정한 CustomUserDetails에서 사용자 ID 추출
			if (authentication.getPrincipal() instanceof CustomUserDetails) {
				CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
				return Optional.ofNullable(userDetails.getUserId());
			}

			return Optional.empty();
		};
	}

}