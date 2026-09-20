package com.foodapp.orderservice.controller;

import com.foodapp.orderservice.exception.GlobalExceptionHandler;
import com.foodapp.orderservice.service.PopularMenuItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({
        PopularMenuItemController.class,
        GlobalExceptionHandler.class
})
class PopularMenuItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PopularMenuItemService service;

    @Test
    void getPopularMenuItems_shouldReturnIds()
            throws Exception {

        when(service.getPopularMenuItemIds(3))
                .thenReturn(
                        List.of(
                                30L,
                                20L,
                                10L
                        )
                );

        mockMvc.perform(
                        get("/api/popular/menu-items")
                                .param("limit", "3")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()")
                        .value(3))
                .andExpect(jsonPath("$[0]")
                        .value(30))
                .andExpect(jsonPath("$[1]")
                        .value(20))
                .andExpect(jsonPath("$[2]")
                        .value(10));
    }
}