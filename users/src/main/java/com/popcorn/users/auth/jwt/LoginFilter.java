package com.popcorn.users.auth.jwt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.users.auth.jwt.JwtUtil;
import com.popcorn.users.auth.dto.CustomUserDetails;
import com.popcorn.users.auth.dto.LoginRequest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;

    private final JwtUtil jwtUtil;
    
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

        LoginRequest loginRequest = new LoginRequest();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ServletInputStream inputStream = request.getInputStream();
            String messageBody = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            loginRequest = objectMapper.readValue(messageBody, LoginRequest.class);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println(loginRequest.getEmail());

        String username = loginRequest.getEmail();
        String password = loginRequest.getPassword();

		//스프링 시큐리티에서 username과 password를 검증하기 위해서는 token에 담아야 함
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password, null);

		//token에 담은 검증을 위한 AuthenticationManager로 전달
        return authenticationManager.authenticate(authToken);
    }

    //로그인 성공시 실행하는 메소드 (여기서 JWT를 발급하면 됨)
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) {

        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

        Long userId = customUserDetails.getUserId();
        String username = customUserDetails.getUsername();
        
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();

       // String role = auth.getAuthority();
        String role = auth.getAuthority().replace("ROLE_", ""); // ROLE_USER -> enum type USER 로 바꿈
        String token = jwtUtil.createJwt(userId,username, role, 60*60*100L);

        response.addHeader("Authorization", "Bearer " + token);

    }

	//로그인 실패시 실행하는 메소드
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {

        // 로그인 실패시 응답코드 : 401
        response.setStatus(401);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // 인증 실패 사유에 따른 메시지 설정
        String errorMessage = "로그인에 실패했습니다.";
        if (failed.getMessage() != null) {
            if (failed.getMessage().contains("Bad credentials")) {
                errorMessage = "아이디 또는 비밀번호가 잘못되었습니다.";
            } else if (failed.getMessage().contains("User account is locked")) {
                errorMessage = "계정이 잠겨있습니다.";
            } else if (failed.getMessage().contains("User account is disabled")) {
                errorMessage = "비활성화된 계정입니다.";
            } else if (failed.getMessage().contains("User account has expired")) {
                errorMessage = "만료된 계정입니다.";
            }
        }

        // JSON 응답 생성
        ObjectMapper mapper = new ObjectMapper();
        String jsonResponse = mapper.writeValueAsString(new LoginErrorResponse(401, "인증 실패", errorMessage));

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    // 로그인 실패 응답 DTO
    private static class LoginErrorResponse {
        private final int code;
        private final String message;
        private final String detail;

        public LoginErrorResponse(int code, String message, String detail) {
            this.code = code;
            this.message = message;
            this.detail = detail;
        }

        public int getCode() { return code; }
        public String getMessage() { return message; }
        public String getDetail() { return detail; }
    }
    
}
