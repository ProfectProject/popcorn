package com.popcorn.store.global.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;

	public JwtAuthenticationFilter(JwtUtil jwtUtil) {
		this.jwtUtil = jwtUtil;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String authorization = request.getHeader("Authorization");
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authorization.substring("Bearer ".length()).trim();
		if (token.isEmpty()) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			if (jwtUtil.isExpired(token)) {
				filterChain.doFilter(request, response);
				return;
			}

			Long userId = jwtUtil.getUserId(token);
			String role = jwtUtil.getRole(token);
			if (userId == null || role == null || role.isBlank()) {
				filterChain.doFilter(request, response);
				return;
			}

			List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
			UsernamePasswordAuthenticationToken authToken =
				new UsernamePasswordAuthenticationToken(userId, null, authorities);

			SecurityContextHolder.getContext().setAuthentication(authToken);
		} catch (Exception ignored) {
			// Invalid token: keep unauthenticated.
		}

		filterChain.doFilter(request, response);
	}
}
