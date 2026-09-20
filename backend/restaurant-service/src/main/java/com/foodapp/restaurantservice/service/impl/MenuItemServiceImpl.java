package com.foodapp.restaurantservice.service.impl;

import com.foodapp.restaurantservice.dto.CreateMenuItemRequest;
import com.foodapp.restaurantservice.dto.MenuItemResponse;
import com.foodapp.restaurantservice.dto.UpdateMenuItemAvailabilityRequest;
import com.foodapp.restaurantservice.dto.UpdateMenuItemRequest;
import com.foodapp.restaurantservice.entity.MenuItem;
import com.foodapp.restaurantservice.exception.BadRequestException;
import com.foodapp.restaurantservice.exception.ResourceNotFoundException;
import com.foodapp.restaurantservice.repository.MenuItemRepository;
import com.foodapp.restaurantservice.repository.RestaurantRepository;
import com.foodapp.restaurantservice.service.MenuItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;

    public MenuItemServiceImpl(
            MenuItemRepository menuItemRepository,
            RestaurantRepository restaurantRepository
    ) {
        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Override
    @Transactional
    public MenuItemResponse createMenuItem(Long restaurantId, CreateMenuItemRequest request){
        validateRestaurantExists(restaurantId);

        MenuItem menuItem = new MenuItem();

        menuItem.setRestaurantId(restaurantId);
        menuItem.setName(request.name().trim());
        menuItem.setDescription(normalizeOptionalText(request.description()));
        menuItem.setPrice(request.price());
        menuItem.setCategory(normalizeOptionalText(request.category()));
        menuItem.setAvailable(true);

        MenuItem savedMenuItem = menuItemRepository.save(menuItem);

        return toResponse(savedMenuItem);
    }

    @Override
    public MenuItemResponse getMenuItemById(Long id){
        return toResponse(findActiveMenuItem(id));
    }

    @Override
    public List<MenuItemResponse> getMenuItemsByRestaurant(Long restaurantId){
        validateRestaurantExists(restaurantId);

        return menuItemRepository
                .findAllByRestaurantIdAndDeletedAtIsNullOrderByCreatedAtAsc(restaurantId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public MenuItemResponse updateMenuItem(Long id, UpdateMenuItemRequest request){
        MenuItem menuItem = findActiveMenuItem(id);

        if (request.name() != null){
            validateNotBlank(request.name(), "Menu item name cannot be blank.");

            menuItem.setName(request.name().trim());
        }

        if (request.description() != null){
            menuItem.setDescription(request.description().trim());
        }

        if (request.price() != null){
            menuItem.setPrice(request.price());
        }

        if (request.category() != null){
            menuItem.setCategory(request.category().trim());
        }

        MenuItem savedMenuItem = menuItemRepository.save(menuItem);

        return toResponse(savedMenuItem);
    }

    @Override
    @Transactional
    public MenuItemResponse updateAvailability(Long id, UpdateMenuItemAvailabilityRequest request){
        MenuItem menuItem = findActiveMenuItem(id);

        menuItem.setAvailable(request.available());

        MenuItem savedMenuItem = menuItemRepository.save(menuItem);

        return toResponse(savedMenuItem);
    }

    @Override
    @Transactional
    public void deleteMenuItem(Long id) {
        MenuItem menuItem = findActiveMenuItem(id);

        menuItem.setDeletedAt(
                LocalDateTime.now()
        );

        menuItem.setAvailable(false);

        menuItemRepository.save(menuItem);
    }


    private void validateNotBlank(String value, String message){
        if (value.isBlank()){
            throw new BadRequestException(message);
        }
    }

    private MenuItem findActiveMenuItem(Long id){
        return menuItemRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found."));
    }

    private void validateRestaurantExists(Long restaurantId) {
        restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found."));
    }

    private String normalizeOptionalText(String value){
        if (value == null){
            return null;
        }
        return value.trim();
    }

    private MenuItemResponse toResponse(MenuItem menuItem){
        return new MenuItemResponse(
                menuItem.getId(),
                menuItem.getRestaurantId(),
                menuItem.getName(),
                menuItem.getDescription(),
                menuItem.getPrice(),
                menuItem.getCategory(),
                menuItem.isAvailable(),
                menuItem.getCreatedAt(),
                menuItem.getUpdatedAt()
        );
    }
}
