package com.popcorn.common.filter;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String userIdHeader = request.getHeader("X-User-Id");
            String roleHeader = resolveRoleHeader(request);
            String emailHeader = request.getHeader("X-User-Email");

            if (userIdHeader != null && roleHeader != null) {
                Long userId = Long.valueOf(userIdHeader);
                String authority = roleHeader.startsWith("ROLE_") ? roleHeader : "ROLE_" + roleHeader;
                PassportPrincipal principal = new PassportPrincipal(userId, roleHeader, emailHeader);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority(authority))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveRoleHeader(HttpServletRequest request) {
        String roleHeader = request.getHeader("X-User-Role");
        if (roleHeader != null) {
            return roleHeader;
        }
        return request.getHeader("X-Role");
    }
}