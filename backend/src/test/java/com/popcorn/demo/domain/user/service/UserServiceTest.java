package com.popcorn.demo.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.hibernate.usertype.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.popcorn.demo.domain.users.dto.SignupRequest;
import com.popcorn.demo.domain.users.dto.SignupResponse;
import com.popcorn.demo.domain.users.entity.User;
import com.popcorn.demo.domain.users.entity.enums.UserRole;
import com.popcorn.demo.domain.users.repository.UserRepository;
import com.popcorn.demo.domain.users.service.UserService;

//red : 실패하는 테스트 먼저 작성

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    @Test
    //회원가입 성공
    void testRegisterUser_Success() {
        // given
        SignupRequest request = new SignupRequest();
        request.setEmail("test@example.com");
        request.setPassword("1234");
        request.setPasswordCheck("1234");
        request.setPhone("01012345678");
        request.setName("test");
        request.setRole(UserRole.CUSTOMER);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("1234"))
                .thenReturn("ENC_PW");

        User savedUser = new User();
        savedUser.setEmail(request.getEmail());
        savedUser.setPassword(request.getPassword());
        savedUser.setPhone(request.getPhone());
        savedUser.setName(request.getName());
        savedUser.setRole(request.getRole());

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        // when
        SignupResponse response = userService.register(request);

        // then 콘솔 검증
        System.out.println("===== User 저장 확인 =====");
        System.out.println("email: " + response.getEmail());
        System.out.println("name: " + response.getName());
        System.out.println("role: " + response.getRole());

        // then
        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
        assertEquals("test", response.getName());
        assertEquals(UserRole.CUSTOMER, response.getRole());

        //User 저장 검증
        verify(userRepository).save(any(User.class));
    }

    @Test
    // 이메일 중복이면 회원가입 실패
    void testRegisterUser_EmailAlreadyExists_ThrowsException() {
        // given
        SignupRequest request = new SignupRequest();
        request.setEmail("test@example.com");
        request.setPassword("1234");
        request.setPasswordCheck("1234");
        request.setPhone("01012345678");
        request.setName("test");
        request.setRole(UserRole.CUSTOMER);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(new User()));

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.register(request);
        });

        // when & then : 콘솔 검증 
        try {
            userService.register(request);
            System.out.println("❌ 테스트 실패: 예외가 발생하지 않음");
        } catch (RuntimeException e) {
            if ("Email already exists".equals(e.getMessage())) {
                System.out.println("✅ 테스트 성공: 이메일 중복 예외 발생");
            } else {
                System.out.println("❌ 테스트 실패: 예외 메시지 불일치 - " + e.getMessage());
            }
        }

        assertEquals("Email already exists", exception.getMessage());
    }
}

