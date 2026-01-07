package com.popcorn.demo.domain.auth.dto;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.popcorn.demo.domain.users.entity.User;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {
    private final User userEntity;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        Collection<GrantedAuthority> collection = new ArrayList<>();

        /*collection.add(new GrantedAuthority() {

            @Override
            public String getAuthority() {

                return userEntity.getRole().name();
            }
        });*/
        collection.add(new SimpleGrantedAuthority("ROLE_" + userEntity.getRole().name()));
        return collection;
    }

    @Override
    public String getPassword() {

        return userEntity.getPassword();
    }

    @Override
    public String getUsername() {

        return userEntity.getEmail();
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

    // JWT 인증 후 사용자 정보 접근을 위한 getter 메서드
    public User getUserEntity() {
        return userEntity;
    }

    public Long getUserId() {
        return userEntity.getUserId();
    }

    public String getRole() {
        return userEntity.getRole().name();
    }

}
