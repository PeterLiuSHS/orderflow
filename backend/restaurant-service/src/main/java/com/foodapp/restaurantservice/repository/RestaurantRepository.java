package com.foodapp.restaurantservice.repository;

import com.foodapp.restaurantservice.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findByIdAndDeletedAtIsNull(Long id);

    Page<Restaurant> findAllByDeletedAtIsNull(Pageable pageable);

    boolean existsByOwnerUserIdAndDeletedAtIsNull(Long ownerUserId);
}
