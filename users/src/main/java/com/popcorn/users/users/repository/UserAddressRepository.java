package com.popcorn.users.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.popcorn.users.users.entity.UserAddress;
import java.util.List;
import java.util.UUID;

public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> {
    List<UserAddress> findByUserUserId(Long userId);
}
