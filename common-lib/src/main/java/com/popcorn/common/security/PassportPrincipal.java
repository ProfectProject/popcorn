package com.popcorn.common.security;

import java.security.Principal;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Builder;
import lombok.Getter;

/**
 * PopCorn 애플리케이션 사용자 정보를 담는 Principal 클래스
 * JWT 토큰에서 추출된 사용자 정보를 Spring Security Context에서 활용
 */
@Getter
@Builder
public class PassportPrincipal implements UserDetails, Principal {

    private final Long userId;
    private final String email;
    private final String role;
    private final String name;

    /**
     * Spring Security에서 사용하는 권한 목록
     * JWT의 role 필드를 ROLE_prefix를 붙여서 GrantedAuthority로 변환
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    /**
     * 사용자 식별자 (이메일 사용)
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Principal의 getName() 메서드 구현
     */
    @Override
    public String getName() {
        return email;
    }

    // UserDetails 인터페이스 필수 구현 메서드들
    @Override
    public String getPassword() {
        return null; // JWT 기반이므로 패스워드 불필요
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * 편의 메서드들
     */
    public boolean hasRole(String role) {
        return this.role != null && this.role.equals(role);
    }

    public boolean isCustomer() {
        return hasRole("CUSTOMER");
    }

    public boolean isManager() {
        return hasRole("MANAGER");
    }

    public boolean isOwner() {
        return hasRole("OWNER");
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
}