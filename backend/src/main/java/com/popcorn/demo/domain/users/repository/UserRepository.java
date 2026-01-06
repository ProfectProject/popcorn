package com.popcorn.demo.domain.users.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.popcorn.demo.domain.users.entity.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email); //이메일로 사용자 조회
    Optional<User> findByEmailAndIsActiveTrue(String email); // isActive = true인 사용자만 조회

    User findByemail(String email);
}
