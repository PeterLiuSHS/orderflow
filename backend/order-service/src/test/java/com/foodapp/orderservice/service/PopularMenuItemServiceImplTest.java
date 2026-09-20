package com.foodapp.orderservice.service;

import com.foodapp.orderservice.exception.BadRequestException;
import com.foodapp.orderservice.repository.MenuItemPopularityRepository;
import com.foodapp.orderservice.service.impl.PopularMenuItemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopularMenuItemServiceImplTest {

    @Mock
    private MenuItemPopularityRepository repository;

    private PopularMenuItemServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new PopularMenuItemServiceImpl(repository);
    }

    @Test
    void getPopularMenuItemIds_shouldReturnConvertedLongIds() {

        when(repository.getTopMenuItemIds(3))
                .thenReturn(
                        Set.of(
                                "10",
                                "20",
                                "30"
                        )
                );

        List<Long> result =
                service.getPopularMenuItemIds(3);

        assertThat(result)
                .containsExactlyInAnyOrder(
                        10L,
                        20L,
                        30L
                );
    }

    @Test
    void getPopularMenuItemIds_shouldReturnEmptyList_whenRepositoryReturnsNull() {

        when(repository.getTopMenuItemIds(5))
                .thenReturn(null);

        List<Long> result =
                service.getPopularMenuItemIds(5);

        assertThat(result).isEmpty();
    }

    @Test
    void getPopularMenuItemIds_shouldThrowBadRequest_whenLimitIsZero() {

        assertThatThrownBy(
                () -> service.getPopularMenuItemIds(0)
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage(
                        "Limit must be between 1 and 20."
                );
    }

    @Test
    void getPopularMenuItemIds_shouldThrowBadRequest_whenLimitIsTooLarge() {

        assertThatThrownBy(
                () -> service.getPopularMenuItemIds(21)
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage(
                        "Limit must be between 1 and 20."
                );
    }
}