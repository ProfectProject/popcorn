/*package com.popcorn.demo.domain.user.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.popcorn.demo.domain.users.dto.SignupRequest;
import com.popcorn.demo.domain.users.entity.User;
import com.popcorn.demo.domain.users.entity.enums.UserRole;
import com.popcorn.demo.domain.users.repository.UserRepository;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    @Test
    void 이메일로_사용자조회() {
        // given
        // given
        SignupRequest request = new SignupRequest();
        request.setEmail("test@example.com");
        request.setPassword("1234");
        request.setPasswordCheck("1234");
        request.setPhone("01012345678");
        request.setName("test");
        request.setRole(UserRole.CUSTOMER);
        
        User savedUser = new User();
        savedUser.setEmail(request.getEmail());
        savedUser.setPassword(request.getPassword());
        savedUser.setPhone(request.getPhone());
        savedUser.setName(request.getName());
        savedUser.setRole(request.getRole());

        userRepository.save(savedUser);

        // when
        Optional<User> result = userRepository.findByEmail("test@example.com");

        // then
        assertTrue(result.isPresent());
        assertEquals("test", result.get().getName());
    }
}*/

