package com.popcorn.store.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;

class JwtAuthenticationFilterTest {

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void skipsWhenAuthorizationHeaderMissing() throws Exception {
		JwtUtil jwtUtil = mock(JwtUtil.class);
		JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void skipsWhenBearerTokenIsBlank() throws Exception {
		JwtUtil jwtUtil = mock(JwtUtil.class);
		JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer   ");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
		verify(jwtUtil, never()).isExpired(anyString());
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void authenticatesWhenTokenIsValid() throws Exception {
		JwtUtil jwtUtil = mock(JwtUtil.class);
		JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer token");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		when(jwtUtil.isExpired("token")).thenReturn(false);
		when(jwtUtil.getUserId("token")).thenReturn(42L);
		when(jwtUtil.getRole("token")).thenReturn("OWNER");

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertThat(authentication).isNotNull();
		assertThat(authentication.getPrincipal()).isEqualTo(42L);
		assertThat(authentication.getAuthorities())
				.extracting(GrantedAuthority::getAuthority)
				.containsExactly("ROLE_OWNER");
	}

	@Test
	void skipsWhenTokenExpired() throws Exception {
		JwtUtil jwtUtil = mock(JwtUtil.class);
		JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer token");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		when(jwtUtil.isExpired("token")).thenReturn(true);

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
		verify(jwtUtil, never()).getUserId("token");
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}
}
