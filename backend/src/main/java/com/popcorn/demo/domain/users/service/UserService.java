package com.popcorn.demo.domain.users.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.popcorn.demo.domain.users.dto.SignupRequest;
import com.popcorn.demo.domain.users.dto.SignupResponse;
import com.popcorn.demo.domain.users.entity.User;
import com.popcorn.demo.domain.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SignupResponse register(SignupRequest request){
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        if (!request.getPassword().equals(request.getPasswordCheck())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        String encodigPassword = passwordEncoder.encode(request.getPassword());

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(encodigPassword);
        user.setPhone(request.getPhone());
        user.setName(request.getName());
        user.setRole(request.getRole());
        user.setActive(true);

        User savedUser = userRepository.save(user);

        return SignupResponse.builder()
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .role(savedUser.getRole())
                .build();

    }
}
