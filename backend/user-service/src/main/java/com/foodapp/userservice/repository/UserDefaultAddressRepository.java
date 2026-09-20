package com.foodapp.userservice.repository;

import com.foodapp.userservice.entity.UserDefaultAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserDefaultAddressRepository extends JpaRepository<UserDefaultAddress, Long> {

    Optional<UserDefaultAddress> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
