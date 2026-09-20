package com.foodapp.restaurantservice.integration;

import com.foodapp.restaurantservice.entity.MenuItem;
import com.foodapp.restaurantservice.entity.Restaurant;
import com.foodapp.restaurantservice.repository.MenuItemRepository;
import com.foodapp.restaurantservice.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class MenuItemApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @BeforeEach
    void cleanDatabase() {
        menuItemRepository.deleteAll();
        restaurantRepository.deleteAll();
    }

    @Test
    void createMenuItem_shouldPersistAndReturnCreated()
            throws Exception {

        Restaurant restaurant = createRestaurant();

        mockMvc.perform(
                        post(
                                "/api/restaurants/{restaurantId}/menu-items",
                                restaurant.getId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Chicken Burger",
                                          "description": "Crispy chicken burger",
                                          "price": 12.50,
                                          "category": "Burgers"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.restaurantId")
                        .value(restaurant.getId()))
                .andExpect(jsonPath("$.name")
                        .value("Chicken Burger"))
                .andExpect(jsonPath("$.price")
                        .value(12.50))
                .andExpect(jsonPath("$.available")
                        .value(true));

        assertThat(menuItemRepository.findAll())
                .hasSize(1);
    }

    @Test
    void createMenuItem_shouldReturnNotFound_whenRestaurantDoesNotExist()
            throws Exception {

        mockMvc.perform(
                        post("/api/restaurants/999/menu-items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Chicken Burger",
                                          "price": 12.50
                                        }
                                        """)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Restaurant not found."));
    }

    @Test
    void getMenuItemsByRestaurant_shouldReturnOnlyActiveItems()
            throws Exception {

        Restaurant restaurant = createRestaurant();

        MenuItem active =
                createMenuItem(
                        restaurant.getId(),
                        "Chicken Burger"
                );

        MenuItem deleted =
                createMenuItem(
                        restaurant.getId(),
                        "Old Burger"
                );

        deleted.setDeletedAt(
                java.time.LocalDateTime.now()
        );
        deleted.setAvailable(false);
        menuItemRepository.save(deleted);

        mockMvc.perform(
                        get(
                                "/api/restaurants/{restaurantId}/menu-items",
                                restaurant.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()")
                        .value(1))
                .andExpect(jsonPath("$[0].id")
                        .value(active.getId()))
                .andExpect(jsonPath("$[0].name")
                        .value("Chicken Burger"));
    }

    @Test
    void getMenuItemById_shouldReturnPersistedItem()
            throws Exception {

        Restaurant restaurant = createRestaurant();

        MenuItem item =
                createMenuItem(
                        restaurant.getId(),
                        "Chicken Burger"
                );

        mockMvc.perform(
                        get(
                                "/api/menu-items/{id}",
                                item.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("Chicken Burger"))
                .andExpect(jsonPath("$.restaurantId")
                        .value(restaurant.getId()));
    }

    @Test
    void updateMenuItem_shouldOnlyModifyProvidedFields()
            throws Exception {

        Restaurant restaurant = createRestaurant();

        MenuItem item =
                createMenuItem(
                        restaurant.getId(),
                        "Chicken Burger"
                );

        mockMvc.perform(
                        patch(
                                "/api/menu-items/{id}",
                                item.getId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "price": 13.50,
                                          "category": "Sandwiches"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("Chicken Burger"))
                .andExpect(jsonPath("$.price")
                        .value(13.50))
                .andExpect(jsonPath("$.category")
                        .value("Sandwiches"));

        MenuItem updated =
                menuItemRepository
                        .findById(item.getId())
                        .orElseThrow();

        assertThat(updated.getName())
                .isEqualTo("Chicken Burger");

        assertThat(updated.getPrice())
                .isEqualByComparingTo("13.50");

        assertThat(updated.getCategory())
                .isEqualTo("Sandwiches");
    }

    @Test
    void updateAvailability_shouldPersistUnavailableStatus()
            throws Exception {

        Restaurant restaurant = createRestaurant();

        MenuItem item =
                createMenuItem(
                        restaurant.getId(),
                        "Chicken Burger"
                );

        mockMvc.perform(
                        patch(
                                "/api/menu-items/{id}/availability",
                                item.getId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "available": false
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available")
                        .value(false));

        MenuItem updated =
                menuItemRepository
                        .findById(item.getId())
                        .orElseThrow();

        assertThat(updated.isAvailable())
                .isFalse();
    }

    @Test
    void deleteMenuItem_shouldSoftDeleteAndMakeItemUnavailable()
            throws Exception {

        Restaurant restaurant = createRestaurant();

        MenuItem item =
                createMenuItem(
                        restaurant.getId(),
                        "Chicken Burger"
                );

        mockMvc.perform(
                        delete(
                                "/api/menu-items/{id}",
                                item.getId()
                        )
                )
                .andExpect(status().isNoContent());

        MenuItem deleted =
                menuItemRepository
                        .findById(item.getId())
                        .orElseThrow();

        assertThat(deleted.getDeletedAt())
                .isNotNull();

        assertThat(deleted.isAvailable())
                .isFalse();

        mockMvc.perform(
                        get(
                                "/api/menu-items/{id}",
                                item.getId()
                        )
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void menuItem_shouldAllowPriceWithTwoDecimalPlaces()
            throws Exception {

        Restaurant restaurant = createRestaurant();

        mockMvc.perform(
                        post(
                                "/api/restaurants/{restaurantId}/menu-items",
                                restaurant.getId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Coffee",
                                          "price": 4.99,
                                          "category": "Drinks"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price")
                        .value(4.99));

        MenuItem item =
                menuItemRepository.findAll().getFirst();

        assertThat(item.getPrice())
                .isEqualByComparingTo(
                        new BigDecimal("4.99")
                );
    }

    private Restaurant createRestaurant() {
        Restaurant restaurant = new Restaurant();

        restaurant.setOwnerUserId(100L);
        restaurant.setName("Dublin Kitchen");
        restaurant.setPhone("0871234567");
        restaurant.setAddressLine("10 Main Street");
        restaurant.setCity("Dublin");
        restaurant.setPostalCode("D01 TEST");
        restaurant.setApproved(true);
        restaurant.setOpen(true);

        return restaurantRepository.save(restaurant);
    }

    private MenuItem createMenuItem(
            Long restaurantId,
            String name
    ) {
        MenuItem item = new MenuItem();

        item.setRestaurantId(restaurantId);
        item.setName(name);
        item.setDescription("Test description");
        item.setPrice(
                new BigDecimal("12.50")
        );
        item.setCategory("Burgers");
        item.setAvailable(true);

        return menuItemRepository.save(item);
    }
}