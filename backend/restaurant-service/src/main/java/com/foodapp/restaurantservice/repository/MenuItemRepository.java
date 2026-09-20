package com.foodapp.restaurantservice.repository;

import com.foodapp.restaurantservice.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    Optional<MenuItem> findByIdAndDeletedAtIsNull(Long id);

    List<MenuItem> findAllByRestaurantIdAndDeletedAtIsNullOrderByCreatedAtAsc(
            Long restaurantId
    );
}
