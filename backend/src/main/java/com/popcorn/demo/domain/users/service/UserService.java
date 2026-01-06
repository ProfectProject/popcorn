package com.popcorn.demo.domain.users.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.users.dto.SignupRequest;
import com.popcorn.demo.domain.users.dto.SignupResponse;
import com.popcorn.demo.domain.users.dto.UserUpdateRequest;
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

    /**
     * 사용자 ID로 사용자 정보 조회
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
    }

    /**
     * 사용자 정보 업데이트
     */
    @Transactional
    public User updateUser(Long userId, UserUpdateRequest request) {
        User user = getUserById(userId);

        // 전화번호 전처리 (-, 공백 등 제거 후 숫자만 남김)
        String cleanedPhone = null;
        if (request.getPhone() != null) {
            cleanedPhone = request.getPhone().replaceAll("[^0-9]", "");
            // 전화번호 중복 체크 (전처리된 값 기준)
            if (userRepository.findByPhone(cleanedPhone).isPresent()) {
                throw new RuntimeException("Phone number already exists");
            }
        }

        // 업데이트할 필드만 변경
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (cleanedPhone!= null) {
            user.setPhone(cleanedPhone);
        }

        return userRepository.save(user);
    }

    /**
     * 사용자 계정 탈퇴
     */
    @Transactional
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        // 기존 이메일 가져오기
        String originalEmail = user.getEmail();

        // 개인정보 마스킹 : 이메일로
        user.setEmail("deleted_" + userId + "_" + originalEmail);
        user.setName("탈퇴회원"+"("+user.getName()+")");

        user.setActive(false); // 비활성화

        userRepository.save(user);
    }
}
