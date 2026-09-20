package com.foodapp.restaurantservice.integration;

import com.foodapp.restaurantservice.entity.Restaurant;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class RestaurantApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @BeforeEach
    void cleanDatabase() {
        restaurantRepository.deleteAll();
    }

    @Test
    void createRestaurant_shouldPersistAndReturnCreated()
            throws Exception {

        mockMvc.perform(post("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ownerUserId": 10,
                                  "name": "Dublin Kitchen",
                                  "description": "Asian fusion restaurant",
                                  "phone": "0871234567",
                                  "addressLine": "10 Main Street",
                                  "city": "Dublin",
                                  "postalCode": "D01 TEST",
                                  "latitude": 53.3498,
                                  "longitude": -6.2603
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerUserId")
                        .value(10))
                .andExpect(jsonPath("$.name")
                        .value("Dublin Kitchen"))
                .andExpect(jsonPath("$.open")
                        .value(false))
                .andExpect(jsonPath("$.approved")
                        .value(false));

        assertThat(
                restaurantRepository
                        .existsByOwnerUserIdAndDeletedAtIsNull(10L)
        ).isTrue();
    }

    @Test
    void createRestaurant_shouldReturnConflict_whenOwnerAlreadyHasRestaurant()
            throws Exception {

        createRestaurant(10L);

        mockMvc.perform(post("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ownerUserId": 10,
                                  "name": "Second Restaurant",
                                  "phone": "0879999999",
                                  "addressLine": "20 Main Street",
                                  "city": "Dublin",
                                  "postalCode": "D02 TEST"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status")
                        .value(409))
                .andExpect(jsonPath("$.message")
                        .value(
                                "This owner already has an active restaurant."
                        ));
    }

    @Test
    void getRestaurantById_shouldReturnPersistedRestaurant()
            throws Exception {

        Restaurant restaurant =
                createRestaurant(10L);

        mockMvc.perform(
                        get(
                                "/api/restaurants/{id}",
                                restaurant.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("Dublin Kitchen"))
                .andExpect(jsonPath("$.ownerUserId")
                        .value(10));
    }

    @Test
    void updateRestaurant_shouldOnlyModifyProvidedFields()
            throws Exception {

        Restaurant restaurant =
                createRestaurant(10L);

        mockMvc.perform(
                        patch(
                                "/api/restaurants/{id}",
                                restaurant.getId()
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "phone": "0899999999",
                                          "city": "Cork"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone")
                        .value("0899999999"))
                .andExpect(jsonPath("$.city")
                        .value("Cork"))
                .andExpect(jsonPath("$.name")
                        .value("Dublin Kitchen"));

        Restaurant updated =
                restaurantRepository
                        .findById(restaurant.getId())
                        .orElseThrow();

        assertThat(updated.getPhone())
                .isEqualTo("0899999999");
        assertThat(updated.getCity())
                .isEqualTo("Cork");
        assertThat(updated.getName())
                .isEqualTo("Dublin Kitchen");
    }

    @Test
    void unapprovedRestaurant_shouldNotBeAbleToOpen()
            throws Exception {

        Restaurant restaurant =
                createRestaurant(10L);

        mockMvc.perform(
                        patch(
                                "/api/restaurants/{id}/open-status",
                                restaurant.getId()
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "open": true
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(
                                "Restaurant must be approved before it can open."
                        ));
    }

    @Test
    void approvedRestaurant_shouldBeAbleToOpen()
            throws Exception {

        Restaurant restaurant =
                createRestaurant(10L);

        mockMvc.perform(
                        patch(
                                "/api/restaurants/{id}/approval-status",
                                restaurant.getId()
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "approved": true
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved")
                        .value(true));

        mockMvc.perform(
                        patch(
                                "/api/restaurants/{id}/open-status",
                                restaurant.getId()
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "open": true
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open")
                        .value(true));
    }

    @Test
    void revokingApproval_shouldAutomaticallyCloseRestaurant()
            throws Exception {

        Restaurant restaurant =
                createRestaurant(10L);

        restaurant.setApproved(true);
        restaurant.setOpen(true);
        restaurantRepository.save(restaurant);

        mockMvc.perform(
                        patch(
                                "/api/restaurants/{id}/approval-status",
                                restaurant.getId()
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "approved": false
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved")
                        .value(false))
                .andExpect(jsonPath("$.open")
                        .value(false));
    }

    @Test
    void getRestaurants_shouldExcludeSoftDeletedRestaurants()
            throws Exception {

        Restaurant active =
                createRestaurant(10L);

        Restaurant deleted =
                createRestaurant(20L);

        deleted.setDeletedAt(
                java.time.LocalDateTime.now()
        );
        deleted.setOpen(false);

        restaurantRepository.save(deleted);

        mockMvc.perform(
                        get("/api/restaurants")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.content[0].id")
                        .value(active.getId()));
    }

    @Test
    void deleteRestaurant_shouldSoftDeleteAndMakeRestaurantUnavailable()
            throws Exception {

        Restaurant restaurant =
                createRestaurant(10L);

        restaurant.setApproved(true);
        restaurant.setOpen(true);

        restaurantRepository.save(restaurant);

        mockMvc.perform(
                        delete(
                                "/api/restaurants/{id}",
                                restaurant.getId()
                        )
                )
                .andExpect(status().isNoContent());

        Restaurant deleted =
                restaurantRepository
                        .findById(restaurant.getId())
                        .orElseThrow();

        assertThat(deleted.getDeletedAt())
                .isNotNull();

        assertThat(deleted.isOpen())
                .isFalse();

        mockMvc.perform(
                        get(
                                "/api/restaurants/{id}",
                                restaurant.getId()
                        )
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void deletedOwnerShouldBeAbleToCreateNewRestaurant()
            throws Exception {

        Restaurant restaurant =
                createRestaurant(10L);

        restaurant.setDeletedAt(
                java.time.LocalDateTime.now()
        );

        restaurantRepository.save(restaurant);

        mockMvc.perform(post("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ownerUserId": 10,
                                  "name": "New Restaurant",
                                  "phone": "0879999999",
                                  "addressLine": "30 Main Street",
                                  "city": "Dublin",
                                  "postalCode": "D03 TEST"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    private Restaurant createRestaurant(
            Long ownerUserId
    ) {

        Restaurant restaurant =
                new Restaurant();

        restaurant.setOwnerUserId(ownerUserId);
        restaurant.setName("Dublin Kitchen");
        restaurant.setDescription(
                "Asian fusion restaurant"
        );
        restaurant.setPhone("0871234567");
        restaurant.setAddressLine(
                "10 Main Street"
        );
        restaurant.setCity("Dublin");
        restaurant.setPostalCode("D01 TEST");
        restaurant.setLatitude(53.3498);
        restaurant.setLongitude(-6.2603);
        restaurant.setOpen(false);
        restaurant.setApproved(false);

        return restaurantRepository.save(restaurant);
    }
}