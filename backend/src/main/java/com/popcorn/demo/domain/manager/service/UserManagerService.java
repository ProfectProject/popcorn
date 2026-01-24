package com.popcorn.demo.domain.manager.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.users.users.dto.manager.OwnerApproveResponse;
import com.popcorn.users.users.dto.manager.OwnerForceStopResponse;
import com.popcorn.users.users.dto.manager.UserForceStopRequest;
import com.popcorn.users.users.dto.manager.UserForceStopResponse;
import com.popcorn.users.users.entity.User;
import com.popcorn.users.users.entity.enums.UserRole;
import com.popcorn.users.users.repository.UserRepository;
import com.popcorn.demo.domain.manager.handler.ApprovalNotAllowedException;
import com.popcorn.demo.domain.manager.handler.NotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class UserManagerService {

    private final UserRepository userRepository;

    @Transactional
    public OwnerApproveResponse approveOwner(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getRole() != UserRole.OWNER) {
            throw new ApprovalNotAllowedException("승인대상이 아닙니다.");
        }
        if(user.getRole() == UserRole.OWNER){

        }

        user.setRole(UserRole.OWNER);
        user.setActive(false);
        User saved = userRepository.save(user);

        return OwnerApproveResponse.builder()
                .userId(saved.getUserId())
                .role(saved.getRole())
                .active(saved.isActive())
                .build();
    }

    @Transactional
    public OwnerForceStopResponse forceStopOwner(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getRole() != UserRole.OWNER) {
            throw new ApprovalNotAllowedException("승인대상이 아닙니다.");
        }

        if (!user.isActive()) {
            throw new IllegalArgumentException("유효된 회원이 아닙니다.");
        }

        user.setActive(false);
        User saved = userRepository.save(user);

        return OwnerForceStopResponse.builder()
                .userId(saved.getUserId())
                .role(saved.getRole())
                .active(saved.isActive())
                .build();
    }

    @Transactional
    public UserForceStopResponse forceStopUser(Long userId, UserForceStopRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getRole() == UserRole.OWNER) {
            throw new ApprovalNotAllowedException("승인대상이 아닙니다.");
        }

        user.setActive(false);
        User saved = userRepository.save(user);

        return UserForceStopResponse.builder()
                .userId(saved.getUserId())
                .role(saved.getRole())
                .active(saved.isActive())
                .build();
    }
}
