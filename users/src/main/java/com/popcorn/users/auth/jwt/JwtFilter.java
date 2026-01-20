package com.popcorn.users.auth.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.popcorn.users.auth.dto.CustomUserDetails;
import com.popcorn.users.users.entity.User;
import com.popcorn.users.users.entity.enums.UserRole;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
				
		String path = request.getServletPath();
        // 로그인, 회원가입 요청은 필터 제외
        if (path.startsWith("/api/users/v1/users/signup") ||
            path.startsWith("/api/users/v1/auth/login")  ||
            path.startsWith("/api/auth/login") )  {
            filterChain.doFilter(request, response);
            return;
        }

        //request에서 Authorization 헤더를 찾음
        String authorization= request.getHeader("Authorization");
				
		//Authorization 헤더 검증
        if (authorization == null || !authorization.startsWith("Bearer ")) {

            System.out.println("token null");
            filterChain.doFilter(request, response);
						
			//조건이 해당되면 메소드 종료 (필수)
            return;
        }

        System.out.println("authorization now");
		//Bearer 부분 제거 후 순수 토큰만 획득
        String[] parts = authorization.split(" ");
        if (parts.length != 2 || parts[1].trim().isEmpty()) {
            System.out.println("Invalid token format");
            filterChain.doFilter(request, response);
            return;
        }
        String token = parts[1];

        // 디버깅을 위한 로그 추가
        System.out.println("Full authorization header: " + authorization);
        System.out.println("Extracted token: " + token);
        System.out.println("Token length: " + token.length());

		//토큰 소멸 시간 검증
        try {
            if (jwtUtil.isExpired(token)) {

                System.out.println("token expired");
                filterChain.doFilter(request, response);

			//조건이 해당되면 메소드 종료 (필수)
                return;
            }
        } catch (Exception e) {
            System.out.println("JWT parsing error: " + e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        //토큰에서 username과 role 획득
        Long userId;
        String username;
        String role;
        try {
            userId = jwtUtil.getUserId(token);
            username = jwtUtil.getUsername(token);
            role = jwtUtil.getRole(token);
        } catch (Exception e) {
            System.out.println("JWT user info extraction error: " + e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        log.info("[JWTFILTER] 토큰에서 userID 획득: {}", userId);
        log.info("[JWTFILTER] 토큰에서 이메일 획득: {}", username);
        log.info("[JWTFILTER] 토큰에서 권한 획득: {}", role);

        // 필수 정보가 null인 경우 인증 실패 처리
        if (userId == null || username == null || role == null) {
            System.out.println("JWT token contains null values - authentication failed");
            filterChain.doFilter(request, response);
            return;
        }

        //SecurityContext에 저장할 Authentication 객체 만들기
        //userEntity를 생성하여 값 set
        User userEntity = new User();
        userEntity.setUserId(userId);
        userEntity.setEmail(username);
        userEntity.setPassword("temppassword");

        try {
            userEntity.setRole(UserRole.valueOf(role)); // String -> UserRole 변환
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid role value: " + role);
            filterChain.doFilter(request, response);
            return;
        }

        //UserDetails에 회원 정보 객체 담기
        CustomUserDetails customUserDetails = new CustomUserDetails(userEntity);

		//스프링 시큐리티 인증 토큰 생성
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
		//세션에 사용자 등록
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}
