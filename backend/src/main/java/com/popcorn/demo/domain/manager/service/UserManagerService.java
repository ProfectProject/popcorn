package com.popcorn.demo.domain.manager.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.users.dto.manager.OwnerApproveResponse;
import com.popcorn.demo.domain.users.dto.manager.UserForceStopRequest;
import com.popcorn.demo.domain.users.dto.manager.UserForceStopResponse;
import com.popcorn.demo.domain.users.entity.User;
import com.popcorn.demo.domain.users.entity.enums.UserRole;
import com.popcorn.demo.domain.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class UserManagerService {

    private final UserRepository userRepository;

    @Transactional
    public OwnerApproveResponse approveOwner(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        user.setRole(UserRole.OWNER);
        user.setActive(true);
        User saved = userRepository.save(user);

        return OwnerApproveResponse.builder()
                .userId(saved.getUserId())
                .role(saved.getRole())
                .active(saved.isActive())
                .build();
    }

    @Transactional
    public UserForceStopResponse forceStopUser(Long userId, UserForceStopRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        user.setActive(false);
        User saved = userRepository.save(user);

        return UserForceStopResponse.builder()
                .userId(saved.getUserId())
                .active(saved.isActive())
                .build();
    }
}
