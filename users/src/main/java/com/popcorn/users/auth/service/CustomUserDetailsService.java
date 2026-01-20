package com.popcorn.users.auth.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.popcorn.users.auth.dto.CustomUserDetails;
import com.popcorn.users.users.entity.User;
import com.popcorn.users.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        log.info("[LOGIN] 입력된 이메일: {}", email);

        User userData = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[LOGIN] 이메일 존재하지 않음: {}", email);
                    return new UsernameNotFoundException("해당 이메일의 유저가 존재하지 않습니다.");
                });

        log.info("[LOGIN] 이메일 존재 확인 완료: {}", email);

        //UserDetails에 담아서 return하면 AutneticationManager가 검증 함
        return new CustomUserDetails(userData);
    }

}
